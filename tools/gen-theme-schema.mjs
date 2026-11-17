// Generates the JSON Schema of theme.json from the generated property tables.
//
// usage: node tools/gen-theme-schema.mjs <GeneratedThemeProps.kt> <ThemeDocument.kt> <out.json>
//
// The component part (every component, its slots and each property with its kind) comes from GeneratedThemeProps.kt,
// so it can never drift from what the compiler accepts; the document part (metadata, tokens, parameters, assets) is
// written below. The version comes from THEME_SCHEMA_VERSION in ThemeDocument.kt.
// Run it after regenerating GeneratedThemeProps.kt or changing the format, and bump THEME_SCHEMA_VERSION (semver).

import { readFileSync, writeFileSync } from 'node:fs';

const [propsFile, docFile, out] = process.argv.slice(2);
if (!propsFile || !docFile || !out) {
  console.error('usage: gen-theme-schema.mjs <GeneratedThemeProps.kt> <ThemeDocument.kt> <out.json>');
  process.exit(2);
}
const props = readFileSync(propsFile, 'utf8');
const version = readFileSync(docFile, 'utf8').match(/const val THEME_SCHEMA_VERSION = "(\d+\.\d+\.\d+)"/)?.[1];
if (!version) throw new Error('THEME_SCHEMA_VERSION not found');
const major = version.split('.')[0];

// ---- parse the generated Kotlin (line based) ------------------------------------------------------------------------
const fnFields = new Map(); // applyXxx -> [{name, kind}]
const comps = new Map(); // component -> [{variant|null, fn}]
let current = null;
for (const line of props.split('\n')) {
  const fn = line.match(/^internal fun (apply\w+)\(/);
  if (fn) {
    current = [];
    fnFields.set(fn[1], current);
    continue;
  }
  const field = current && line.match(/^\s+(\w+) = r\.(\w+)\(/);
  if (field) current.push({ name: field[1], kind: field[2] });
  const slot = line.match(/stateProp\("(\w+)",.*::(apply\w+),\s*(null|"\w+")\)/);
  if (slot) {
    const list = comps.get(slot[1]) ?? [];
    list.push({ variant: slot[3] === 'null' ? null : slot[3].slice(1, -1), fn: slot[2] });
    comps.set(slot[1], list);
  }
  const single = line.match(/singleProp\("(\w+)",.*::(apply\w+),/);
  if (single) comps.set(single[1], [{ variant: null, fn: single[2], single: true }]);
}
if (comps.size < 20) throw new Error(`only found ${comps.size} components; the generated file format changed`);

// ---- value kinds ----------------------------------------------------------------------------------------------------
const KIND = {
  color: 'color', colors: 'colors', brush: 'brush', dp: 'number', dpOrNull: 'numberOrNull', float: 'number', floatOrNull: 'numberOrNull', int: 'number', intOrNull: 'numberOrNull',
  long: 'number', bool: 'bool', shape: 'shape', shapeOrNull: 'shapeOrNull', textStyle: 'textStyle', textStyleOrNull: 'textStyleOrNull', padding: 'padding', dpSize: 'size',
  motion: 'motion', buttonStyle: 'any',
};
const propSchema = (reader) => {
  const k = KIND[reader];
  if (!k) throw new Error(`no schema kind for reader r.${reader}`);
  return { $ref: `#/$defs/${k}` };
};

const propsOf = (fns) => {
  const merged = new Map();
  for (const fn of fns) for (const f of fnFields.get(fn) ?? []) merged.set(f.name, propSchema(f.kind));
  return Object.fromEntries([...merged].sort(([a], [b]) => a.localeCompare(b)));
};

const STATES = ['default', 'hovered', 'focused', 'pressed', 'disabled', 'selected', 'error'];
// One property set per slot (and one merged set per multi-slot component), defined once in $defs and referenced.
const bagDefs = {};
const propBag = (fns) => {
  const key = fns.length === 1 ? fns[0].replace(/^apply|ThemeProperties$|Properties$/g, '') : `${fns[0].replace(/^apply|ThemeProperties$|Properties$/g, '')}All`;
  bagDefs[`props.${key}`] ??= { type: 'object', properties: propsOf(fns), additionalProperties: false };
  return { $ref: `#/$defs/props.${key}` };
};
const stateMap = (fns) => ({
  type: 'object',
  description: 'Properties per interaction state; they win over `base` in that state.',
  properties: Object.fromEntries(STATES.map((s) => [s, propBag(fns)])),
  additionalProperties: false,
});
const rules = {
  type: 'array',
  description: 'Conditional overrides, applied last: `{ "when": <condition>, "set": { ...properties } }`.',
  items: { type: 'object', required: ['when', 'set'], properties: { when: { $ref: '#/$defs/condition' }, set: { type: 'object' } }, additionalProperties: false },
};
const varsDecl = { type: 'object', description: 'Variables local to this component (they shadow global ones).', additionalProperties: { $ref: '#/$defs/varDecl' } };

const components = {};
for (const [name, slots] of [...comps].sort(([a], [b]) => a.localeCompare(b))) {
  const allFns = slots.map((s) => s.fn);
  const sch = {
    type: 'object',
    description: `Theme of the \`${name}\` component.`,
    properties: slots[0].single ? { base: propBag(allFns), rules, vars: varsDecl } : { base: propBag(allFns), states: stateMap(allFns), rules, vars: varsDecl },
    additionalProperties: false,
  };
  if (slots.length > 1 || slots[0].variant) {
    sch.properties.variants = {
      type: 'object',
      description: 'One entry per slot/variant; a `base` above applies to all of them, so put slot-specific properties here.',
      properties: Object.fromEntries(slots.map((s) => [s.variant, {
        type: 'object',
        properties: { base: propBag([s.fn]), states: stateMap([s.fn]) },
        additionalProperties: false,
      }])),
      additionalProperties: false,
    };
  }
  components[name] = sch;
}

// ---- the document --------------------------------------------------------------------------------------------------
const hex = '^#([0-9A-Fa-f]{3}|[0-9A-Fa-f]{6}|[0-9A-Fa-f]{8})$';
const refPattern = '^\\{[^{}]+\\}$';
const common = [
  { type: 'string', pattern: refPattern, description: 'A reference: `{tokens.color.x}`, `{vars.x}`, `{params.x}`, `{palette.x}`, `{ctx.x}`.' },
  { type: 'object', required: ['light', 'dark'], properties: { light: {}, dark: {} }, additionalProperties: false, description: 'A value per appearance.' },
  { type: 'array', minItems: 1, prefixItems: [{ type: 'string' }], description: 'An expression: `["alpha", color, 0.5]`, `["match", ...]`, `["linear", angle, stops...]`...' },
];
const withNull = (list) => [...list, { type: 'null' }];
const num = { anyOf: [{ type: 'number' }, { type: 'string' }] };
const defs = {
  color: { description: 'A colour: #RGB, #RRGGBB, #RRGGBBAA, a reference, an appearance pair or an expression. `null` clears a nullable colour.', anyOf: withNull([{ type: 'string', pattern: hex }, ...common]) },
  colors: { description: 'A list of colours.', anyOf: [{ type: 'array', items: { $ref: '#/$defs/color' } }, ...common] },
  brush: { description: 'A colour or a gradient expression (`linear`, `gradient`, `radial`, `sweep`, `gel`).', anyOf: withNull([{ type: 'string', pattern: hex }, ...common]) },
  number: { description: 'A number (dp, fraction, count) or an expression.', anyOf: [{ type: 'number' }, ...common] },
  numberOrNull: { anyOf: withNull([{ type: 'number' }, ...common]) },
  bool: { anyOf: [{ type: 'boolean' }, ...common] },
  shape: { description: 'A corner radius, "pill", "rect" or four corner radii.', anyOf: [{ type: 'number' }, { enum: ['pill', 'rect'] }, { type: 'array', items: num, minItems: 4, maxItems: 4 }, ...common] },
  shapeOrNull: { anyOf: withNull([{ $ref: '#/$defs/shape' }]) },
  padding: { description: 'A number, [horizontal, vertical], [start, top, end, bottom] or an object.', anyOf: [{ type: 'number' }, { type: 'array', items: num, minItems: 2, maxItems: 4 }, { type: 'object' }, ...common] },
  size: { description: '[width, height].', anyOf: [{ type: 'array', items: num, minItems: 2, maxItems: 2 }, ...common] },
  textStyle: {
    description: 'Merged onto the default text style.',
    anyOf: [{
      type: 'object',
      properties: {
        fontSize: { $ref: '#/$defs/number' }, lineHeight: { $ref: '#/$defs/number' }, fontWeight: { $ref: '#/$defs/number' }, letterSpacing: { $ref: '#/$defs/number' },
        fontStyle: { enum: ['normal', 'italic'] }, fontFamily: { type: 'string' }, color: { $ref: '#/$defs/color' }, textDecoration: { type: 'string' },
        shadow: { type: 'object', properties: { color: { $ref: '#/$defs/color' }, offsetX: { $ref: '#/$defs/number' }, offsetY: { $ref: '#/$defs/number' }, blurRadius: { $ref: '#/$defs/number' } }, additionalProperties: false },
      },
      additionalProperties: false,
    }, ...common],
  },
  textStyleOrNull: { anyOf: withNull([{ $ref: '#/$defs/textStyle' }]) },
  motion: { description: 'Button motion: stateChangeMillis, hoverScale, pressedScale, scaleDampingRatio...', anyOf: [{ type: 'object' }, ...common] },
  any: {},
  condition: {
    description: 'A rule condition: `{ "state": "hovered" }`, `{ "all": [...] }`, `{ "any": [...] }`, `{ "not": c }`, `{ "variant": "primary" }`, `{ "var": "ctx.env.dark", "eq": true }`.',
    anyOf: [{ type: 'object' }, { type: 'boolean' }, { type: 'string' }],
  },
  varDecl: {
    type: 'object',
    required: ['type'],
    properties: {
      type: { enum: ['color', 'number', 'dp', 'bool', 'boolean', 'string', 'enum'] },
      default: {}, value: { description: 'A derived expression (vars only).' }, min: { type: 'number' }, max: { type: 'number' },
      options: { type: 'array', items: { anyOf: [{ type: 'string' }, { $ref: '#/$defs/option' }] } },
    },
    anyOf: [{ required: ['default'] }, { required: ['value'] }],
  },
  option: { type: 'object', required: ['value'], properties: { value: { type: 'string' }, label: { type: 'string', maxLength: 60 }, color: { type: 'string', pattern: hex } }, additionalProperties: false },
  parameter: {
    type: 'object',
    required: ['type', 'default', 'label'],
    properties: {
      type: { enum: ['color', 'number', 'bool', 'string', 'enum'] },
      label: { type: 'string', maxLength: 60 }, description: { type: 'string', maxLength: 200 }, default: {}, min: { type: 'number' }, max: { type: 'number' }, step: { type: 'number', exclusiveMinimum: 0 },
      options: { type: 'array', items: { anyOf: [{ type: 'string' }, { $ref: '#/$defs/option' }] } },
      presets: { type: 'array', items: { type: 'string', pattern: hex }, maxItems: 16 },
    },
    additionalProperties: false,
  },
  url: { type: 'string', pattern: '^https?://[^\\s/]+', maxLength: 300 },
  glyph: { type: 'string', description: 'A single character or `U+XXXX`.', pattern: '^(U\\+[0-9A-Fa-f]{4,6}|.)$' },
};

const colorRoles = ['background', 'surface', 'raised', 'inset', 'text', 'muted', 'border', 'highlight', 'accent', 'accentTop', 'accentBottom', 'onAccent', 'success', 'danger', 'scrim'];
const typeStyles = ['displaySmall', 'headlineLarge', 'headlineMedium', 'headlineSmall', 'titleLarge', 'titleMedium', 'titleSmall', 'bodyLarge', 'bodyMedium', 'bodySmall', 'labelLarge', 'labelMedium', 'labelSmall'];

const schema = {
  $schema: 'https://json-schema.org/draft/2020-12/schema',
  $id: `https://docs.spotube.cc/schemas/theme/${version}.json`,
  title: 'Spotube theme (theme.json)',
  description: `Theme format ${version}. Semver: a theme of the same major always loads; a newer minor loads with its newer parts ignored. Latest ${major}.x: https://docs.spotube.cc/schemas/theme/${major}.json`,
  'x-schemaVersion': version,
  type: 'object',
  required: ['schema', 'id', 'name', 'version', 'author'],
  properties: {
    $schema: { type: 'string', description: `Editor hint: https://docs.spotube.cc/schemas/theme/${major}.json` },
    schema: { type: 'string', pattern: '^(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})$', description: `The theme schema version this theme is written for (semver), for example "${version}".` },
    id: { type: 'string', pattern: '^[A-Za-z0-9._-]{1,48}$', description: 'Stable identifier; must equal the package folder name.' },
    name: { type: 'string', minLength: 1 },
    version: { type: 'string', pattern: '^[0-9A-Za-z.+_-]{1,32}$', description: "The theme's own version (not the schema version)." },
    author: { type: 'string', maxLength: 80 },
    description: { type: 'string', maxLength: 500 },
    repository: { $ref: '#/$defs/url' },
    bugs: { $ref: '#/$defs/url' },
    homepage: { $ref: '#/$defs/url' },
    contact: { type: 'string', maxLength: 200, description: 'An email address or an http(s) URL.' },
    license: { type: 'string', maxLength: 60, description: 'SPDX identifier; ship the full text as LICENSE.' },
    logo: { type: 'string', pattern: '\\.png$', description: 'Path of a .png (max 512x512) inside the package.' },
    parameters: { type: 'object', description: 'Settings offered to users (`{params.name}`), at most 24.', propertyNames: { pattern: '^[A-Za-z][A-Za-z0-9_]{0,31}$' }, maxProperties: 24, additionalProperties: { $ref: '#/$defs/parameter' } },
    vars: { type: 'object', description: "The author's private token store (`{vars.name}`).", additionalProperties: { $ref: '#/$defs/varDecl' } },
    assets: {
      type: 'object',
      properties: {
        fonts: { type: 'object', description: 'Font families: `{ "Family": [ { "src": "fonts/x.ttf", "weight": 400, "style": "italic" } ] }`.', additionalProperties: { type: 'array', items: { type: 'object', required: ['src'], properties: { src: { type: 'string', pattern: '\\.(ttf|otf)$' }, weight: { type: 'integer', minimum: 1, maximum: 1000 }, style: { enum: ['normal', 'italic'] } }, additionalProperties: false } } },
        icons: { type: 'object', description: 'Per-icon replacements keyed by the app icon vector name.', additionalProperties: { type: 'object', properties: { glyph: { $ref: '#/$defs/glyph' }, font: { type: 'string' }, image: { type: 'string', pattern: '\\.png$' }, tint: { type: 'boolean' } }, additionalProperties: false } },
        iconPack: { type: 'object', description: 'An icon font plus a glyph per icon role (Lucide names), restyling every icon.', required: ['font', 'icons'], properties: { font: { type: 'string' }, icons: { type: 'object', additionalProperties: { $ref: '#/$defs/glyph' } } }, additionalProperties: false },
      },
      additionalProperties: false,
    },
    tokens: {
      type: 'object',
      properties: {
        color: { type: 'object', properties: Object.fromEntries(colorRoles.map((r) => [r, { $ref: '#/$defs/color' }])), additionalProperties: false },
        shape: { type: 'object', properties: { small: { $ref: '#/$defs/shape' }, medium: { $ref: '#/$defs/shape' }, large: { $ref: '#/$defs/shape' } }, additionalProperties: false },
        type: { type: 'object', properties: { family: { type: 'string' }, scale: { $ref: '#/$defs/number' }, styles: { type: 'object', properties: Object.fromEntries(typeStyles.map((s) => [s, { $ref: '#/$defs/textStyle' }])), additionalProperties: false } }, additionalProperties: false },
        interaction: { type: 'object', description: 'Hover/press lighting of rows and tabs.', properties: { lighting: { $ref: '#/$defs/bool' }, hover: { $ref: '#/$defs/brush' }, press: { $ref: '#/$defs/brush' } }, additionalProperties: false },
        surface: { type: 'object', description: 'Flatten every Millennium surface.', properties: { shadows: { $ref: '#/$defs/bool' }, glow: { $ref: '#/$defs/bool' } }, additionalProperties: false },
        app: { type: 'object', description: 'The backdrop behind every screen.', properties: { background: { $ref: '#/$defs/brush' } }, additionalProperties: false },
      },
      additionalProperties: false,
    },
    defaults: { type: 'object', description: 'Properties applied to every component that has them, for example `{ "shape": 12 }`.' },
    components: { type: 'object', properties: components, additionalProperties: false },
  },
  additionalProperties: false,
  $defs: { ...defs, ...bagDefs },
};

writeFileSync(out, `${JSON.stringify(schema, null, 2)}\n`);
console.log(`theme schema ${version}: ${comps.size} components, ${[...fnFields.values()].reduce((a, f) => a + f.length, 0)} properties -> ${out}`);

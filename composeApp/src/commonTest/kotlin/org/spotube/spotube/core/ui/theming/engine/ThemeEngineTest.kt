/*
 * Copyright (C) 2026 Kingkor Roy Tirtho and Spotube Contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.spotube.spotube.core.ui.theming.engine

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.spotube.millennium.theme.MillenniumColorSchemes
import org.spotube.millennium.theme.MillenniumDarkThemeComponents
import org.spotube.millennium.theme.MillenniumPalette
import org.spotube.millennium.theme.resolve
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ThemeEngineTest {
    private fun compile(
        json: String,
        dark: Boolean = true,
        compact: Boolean = false,
        user: Map<String, String> = emptyMap(),
    ): CompiledTheme {
        val problems = LinkedHashSet<String>()
        val doc = parseThemeDocument(json, problems)
        return compileTheme(
            doc = doc, parameters = user, env = ThemeEnv(dark, compact),
            baseColors = MillenniumColorSchemes().resolve(MillenniumPalette.Blue, dark),
            baseComponents = MillenniumDarkThemeComponents, parseProblems = problems,
        )
    }

    private val base = MillenniumDarkThemeComponents

    @Test
    fun nullOrInvalidJsonFallsBackToBase() {
        val t = compile("{ not json")
        assertTrue(t.problems.isNotEmpty())
        assertEquals(base, t.components)
    }

    @Test
    fun variablesFeedTokensAndComponents() {
        val t = compile(
            """{"id":"t","vars":{"r":{"type":"number","default":20,"min":0,"max":30},
               "accent":{"type":"color","default":"#112233"}},
               "tokens":{"color":{"accent":"{vars.accent}"}},
               "components":{"card":{"base":{"shape":"{vars.r}"}}}}""",
        )
        assertEquals(Color(0xFF112233), t.colors.accent)
        assertEquals(RoundedCornerShape(20.dp), t.components.cardTheme.states.default.shape)
    }

    @Test
    fun parameterValuesAreClampedValidatedAndOnlyParametersCanBeOverridden() {
        val json = """{"id":"t",
            "parameters":{"r":{"type":"number","default":10,"min":0,"max":30},
                          "mode":{"type":"enum","default":"a","options":["a","b"]}},
            "vars":{"fixed":{"type":"number","default":5}},
            "components":{"card":{"base":{"shape":"{params.r}","borderWidth":"{vars.fixed}"}}}}"""
        fun shape(user: Map<String, String>) = compile(json, user = user).components.cardTheme.states.default.shape
        assertEquals(RoundedCornerShape(10.dp), shape(emptyMap()))
        assertEquals(RoundedCornerShape(30.dp), shape(mapOf("r" to "99")))   // clamped to max
        assertEquals(RoundedCornerShape(10.dp), shape(mapOf("r" to "oops")))  // invalid -> default
        // A user value can never replace a private var, even by name.
        assertEquals(5.dp, compile(json, user = mapOf("fixed" to "20")).components.cardTheme.states.default.borderWidth)
    }

    @Test
    fun enumParametersAndDerivedVarsFollowTheChoice() {
        val json = """{"id":"t","parameters":{"d":{"type":"enum","default":"cozy","options":["tight","cozy"]}},
            "vars":{"pad":{"type":"number","value":["match","{params.d}",{"tight":4,"cozy":12}]}},
            "components":{"card":{"base":{"contentSpacing":"{vars.pad}"}}}}"""
        assertEquals(12.dp, compile(json).components.cardTheme.states.default.contentSpacing)
        assertEquals(4.dp, compile(json, user = mapOf("d" to "tight")).components.cardTheme.states.default.contentSpacing)
        assertEquals(12.dp, compile(json, user = mapOf("d" to "nonsense")).components.cardTheme.states.default.contentSpacing)
    }

    @Test
    fun parameterDeclarationsAreValidated() {
        val problems = LinkedHashSet<String>()
        val doc = parseThemeDocument(
            """{"parameters":{"ok":{"type":"number","default":1,"label":"Size","description":"d","step":0.5},
                "bad name":{"type":"number","default":1},"derived":{"type":"number","value":2},"nodefault":{"type":"bool"},
                "choice":{"type":"enum","default":"x","options":[{"value":"x","label":"X","color":"#112233"},"y"],"presets":["#fff","nope"]}}}""",
            problems,
        )!!
        assertEquals(setOf("ok", "choice"), doc.parameters.keys)
        assertEquals("Size", doc.parameters.getValue("ok").label)
        assertEquals(0.5, doc.parameters.getValue("ok").step)
        val choice = doc.parameters.getValue("choice")
        assertEquals(listOf("x", "y"), choice.options)
        assertEquals("X", choice.optionLabels["x"])
        assertEquals("#112233", choice.optionColors["x"])
        assertEquals(listOf("#fff"), choice.presets)
        assertTrue(problems.any { "bad name" in it } && problems.any { "derived" in it })
    }

    @Test
    fun derivedVariablesAndMatch() {
        val t = compile(
            """{"id":"t","vars":{"d":{"type":"enum","options":["compact","roomy"],"default":"roomy"},
               "pad":{"type":"number","value":["match","{vars.d}",{"compact":8,"roomy":18}]}},
               "components":{"card":{"base":{"contentSpacing":"{vars.pad}"}}}}""",
        )
        assertEquals(18.dp, t.components.cardTheme.states.default.contentSpacing)
    }

    @Test
    fun cyclesAreReportedNotLooped() {
        val t = compile("""{"id":"t","vars":{"a":{"type":"number","value":"{vars.b}"},"b":{"type":"number","value":"{vars.a}"}},
            "components":{"card":{"base":{"borderWidth":"{vars.a}"}}}}""")
        assertTrue(t.problems.any { "depends on itself" in it })
        assertEquals(base.cardTheme.states.default.borderWidth, t.components.cardTheme.states.default.borderWidth)
    }

    @Test
    fun defaultsApplyToEveryComponentThatHasTheProp() {
        val t = compile("""{"defaults":{"shape":7}}""")
        assertEquals(RoundedCornerShape(7.dp), t.components.cardTheme.states.default.shape)
        assertEquals(RoundedCornerShape(7.dp), t.components.buttonTheme.primary.hovered.shape)
        assertEquals(RoundedCornerShape(7.dp), t.components.listTileTheme.states.selected.shape)
    }

    @Test
    fun cascadeStateVariantAndRules() {
        val t = compile(
            """{"components":{"button":{
                 "base":{"borderWidth":1},
                 "states":{"hovered":{"borderWidth":2}},
                 "variants":{"primary":{"states":{"hovered":{"borderWidth":3}}}},
                 "rules":[{"when":{"all":[{"state":"pressed"},{"variant":"quiet"}]},"set":{"borderWidth":9}}]}}}""",
        )
        val b = t.components.buttonTheme
        assertEquals(1.dp, b.secondary.default.borderWidth)
        assertEquals(2.dp, b.secondary.hovered.borderWidth)
        assertEquals(3.dp, b.primary.hovered.borderWidth)
        assertEquals(9.dp, b.quiet.pressed.borderWidth)
        assertEquals(1.dp, b.primary.pressed.borderWidth)
    }

    @Test
    fun conditionsReadEnvironmentAndVariables() {
        val json = """{"id":"t","vars":{"glass":{"type":"bool","default":true}},
            "components":{"topAppBar":{"rules":[
              {"when":{"var":"ctx.env.compact"},"set":{"minimumHeight":44}},
              {"when":{"var":"vars.glass","eq":false},"set":{"minimumHeight":10}}]}}}"""
        assertEquals(44.dp, compile(json, compact = true).components.topAppBarTheme.properties.minimumHeight)
        assertEquals(base.topAppBarTheme.properties.minimumHeight, compile(json, compact = false).components.topAppBarTheme.properties.minimumHeight)
        assertEquals(10.dp, compile(json.replace("\"vars\":{\"glass\"","\"parameters\":{\"glass\"").replace("vars.glass","params.glass"), user = mapOf("glass" to "false")).components.topAppBarTheme.properties.minimumHeight)
    }

    @Test
    fun appearanceMapsAndPaletteFunctions() {
        val json = """{"tokens":{"color":{"accent":{"light":"#FF0000","dark":"#00FF00"},
            "surface":["alpha","{palette.text}",0.5]}}}"""
        assertEquals(Color(0xFF00FF00), compile(json, dark = true).colors.accent)
        assertEquals(Color(0xFFFF0000), compile(json, dark = false).colors.accent)
        assertTrue(kotlin.math.abs(compile(json).colors.surface.alpha - 0.5f) < 0.01f)
    }

    @Test
    fun badValuesKeepBaseAndReport() {
        val t = compile("""{"components":{"card":{"base":{"shape":"banana","borderWidth":"x"}}}}""")
        assertEquals(base.cardTheme.states.default.shape, t.components.cardTheme.states.default.shape)
        assertTrue(t.problems.isNotEmpty())
        assertNotNull(t.shapes)
    }

    @Test
    fun deepNestingIsRejected() {
        var expr = "1"
        repeat(55) { expr = """["add",$expr,1]""" }
        val t = compile("""{"components":{"card":{"base":{"borderWidth":$expr}}}}""")
        assertTrue(t.problems.any { "nests deeper" in it })
    }

    // ---- brushes and text styles -------------------------------------------------------------

    @Test
    fun brushLanguageCoversLinearRadialSweepAndGel() {
        val t = compile(
            """{"components":{"button":{"variants":{
                 "primary":{"base":{"background":["linear",135,"#ff0000",["#00ff00",0.25],"#0000ff"]}},
                 "secondary":{"base":{"background":["radial",0.5,0.5,0.7,"#fff","#000"]}},
                 "quiet":{"base":{"background":["sweep",0.5,0.5,"#f00","#00f"]}},
                 "danger":{"base":{"background":["gel","#ff0000","#880000"]}}}}}}""",
        )
        val b = t.components.buttonTheme
        val linear = b.primary.default.background as LinearBrush
        assertEquals(135f, linear.angleDegrees)
        assertEquals(listOf(0f, 0.25f, 1f), linear.positions)
        assertTrue(b.secondary.default.background is RadialBrush)
        assertTrue(b.quiet.default.background is SweepBrush)
        assertNotNull(b.danger.default.background)
        assertEquals(emptyList(), t.problems)
    }

    @Test
    fun missingStopPositionsAreInterpolatedAndOrdered() {
        val stops = listOf(BrushStop(Color.Red, 0.2f), BrushStop(Color.Green), BrushStop(Color.Blue), BrushStop(Color.White, 0.8f))
        assertEquals(listOf(0.2f, 0.4f, 0.6f, 0.8f), resolveStopPositions(stops))
        assertNull(resolveStopPositions(listOf(BrushStop(Color.Red), BrushStop(Color.Blue))))
        assertEquals(listOf(0.5f, 0.5f), resolveStopPositions(listOf(BrushStop(Color.Red, 0.5f), BrushStop(Color.Blue, 0.1f))))
    }

    @Test
    fun badBrushesAreReportedNotApplied() {
        val t = compile("""{"components":{"card":{"base":{"background":["linear","north","#f00","#00f"]}}}}""")
        assertTrue(t.problems.isNotEmpty())
        assertEquals(base.cardTheme.states.default.background, t.components.cardTheme.states.default.background)
    }

    @Test
    fun textStylesTakeShadowColorAndDecoration() {
        val t = compile(
            """{"components":{"topAppBar":{"base":{"titleStyle":{
                 "color":"#112233","textDecoration":"underline",
                 "shadow":{"color":"#000000","offsetX":1,"offsetY":2,"blurRadius":3}}}}}}""",
        )
        val style = t.components.topAppBarTheme.properties.titleStyle
        assertEquals(Color(0xFF112233), style.color)
        assertEquals(androidx.compose.ui.text.style.TextDecoration.Underline, style.textDecoration)
        assertEquals(3f, style.shadow!!.blurRadius)
        assertEquals(androidx.compose.ui.geometry.Offset(1f, 2f), style.shadow!!.offset)
    }

    // ---- metadata ---------------------------------------------------------------------------

    @Test
    fun metadataIsParsedAndLinksAreRestrictedToWebUrls() {
        val problems = LinkedHashSet<String>()
        val doc = parseThemeDocument(
            """{"id":"m","name":"M","version":"1.2.3","author":"A","description":"D","license":"MIT",
                "repository":"https://github.com/x/y","bugs":"javascript:alert(1)","homepage":"file:///etc/passwd",
                "contact":"dev@example.com","logo":"logo.png"}""",
            problems,
        )!!
        assertEquals("1.2.3", doc.meta.version)
        assertEquals("https://github.com/x/y", doc.meta.repository)
        assertNull(doc.meta.bugs)
        assertNull(doc.meta.homepage)
        assertEquals("mailto:dev@example.com", doc.meta.contact)
        assertEquals(2, problems.count { "http(s)" in it })
        assertNull(doc.meta.missingRequired(doc.name))
    }

    @Test
    fun requiredMetadataIsEnforced() {
        fun missing(json: String) = parseThemeDocument(json, LinkedHashSet())!!.let { it.meta.missingRequired(it.name.takeIf { _ -> it.hasExplicitName }) }
        assertEquals("name", missing("""{"version":"1","author":"a"}"""))
        assertEquals("version", missing("""{"name":"n","author":"a"}"""))
        assertEquals("author", missing("""{"name":"n","version":"1"}"""))
        assertNull(missing("""{"name":"n","version":"1","author":"a"}"""))
    }

    @Test
    fun webUrlValidatorRejectsDangerousSchemesAndJunk() {
        for (ok in listOf("https://a.b/c", "http://localhost:8080/x?y#z", "HTTPS://Example.com")) assertEquals(ok, safeWebUrl(ok), ok)
        for (bad in listOf("javascript:alert(1)", "file:///x", "data:text/html,x", "ftp://a.b", "https://", "https:// a.b", "//a.b", "https://a.b/\u0000", "mailto:x@y.z", "https://" + "a".repeat(400))) {
            assertNull(safeWebUrl(bad), bad)
        }
    }

    @Test
    fun unknownPropertiesAreReportedButDefaultsMayNameAnyProp() {
        val t = compile(
            """{"defaults":{"shape":4,"notAThingOnTopBar":1},
                "components":{"card":{"base":{"borderWidht":2,"borderWidth":3}},
                              "button":{"variants":{"primary":{"states":{"hovered":{"backgrund":"#fff"}}}}}}}""",
        )
        assertEquals(setOf("components.card: unknown property \"borderWidht\"", "components.button.primary: unknown property \"backgrund\""), t.problems.toSet())
        assertEquals(androidx.compose.ui.unit.Dp(3f), t.components.cardTheme.states.default.borderWidth)
    }

    @Test
    fun interactionTokensConfigureTheGlobalLighting() {
        assertEquals(true, compile("{}").interaction.enabled)
        val off = compile("""{"tokens":{"interaction":{"lighting":false}}}""").interaction
        assertEquals(false, off.enabled)
        val custom = compile("""{"tokens":{"interaction":{"hover":"#11223344","press":["gradient","#000","#fff"]}}}""").interaction
        assertEquals(true, custom.enabled)
        assertNotNull(custom.hoverBrush)
        assertNotNull(custom.pressBrush)
        assertNull(off.hoverBrush)
    }

    @Test
    fun nullClearsRimAndOrbSoNothingIsDrawn() {
        val t = compile("""{"components":{"topAppBar":{"base":{"separatorColor":null}},"card":{"base":{"bottomRimColor":null,"borderColor":null}}}}""")
        assertNull(t.components.topAppBarTheme.properties.separatorColor)
        assertNull(t.components.cardTheme.states.default.bottomRimColor)
        assertEquals(emptyList(), t.problems)
    }

    @Test
    fun surfaceTokensSwitchShadowsAndGlow() {
        assertEquals(true, compile("{}").surface.shadows)
        val flat = compile("""{"tokens":{"surface":{"shadows":false,"glow":false}}}""").surface
        assertEquals(false, flat.shadows)
        assertEquals(false, flat.glow)
        val noShadow = compile("""{"tokens":{"surface":{"shadows":false}}}""").surface
        assertEquals(true, noShadow.glow)
    }

    @Test
    fun appBackgroundTokenIsABrush() {
        assertNull(compile("{}").appBackground)
        val t = compile("""{"tokens":{"app":{"background":["linear",180,"#112233","#445566"]}}}""")
        assertNotNull(t.appBackground)
        assertEquals(emptyList(), t.problems)
    }

    @Test
    fun iconRolesAreLucideNames() {
        assertEquals("skip-forward", kebabCase("SkipForward"))
        assertEquals("trash-2", kebabCase("Trash2"))
        assertEquals("trash-2", kebabCase("Trash-2"))
        assertEquals("disc-3", kebabCase("Disc3"))
        assertEquals("trash-2", iconRoleByVectorName["IconsaxTrash"])
        assertEquals("house", iconRoleByVectorName["IconsaxHome"])
    }

    private fun parseProblems(json: String): Pair<ThemeDocument?, Set<String>> {
        val problems = linkedSetOf<String>()
        return parseThemeDocument(json, problems) to problems
    }

    @Test
    fun schemaVersionFollowsSemver() {
        assertEquals(SchemaVersion(1, 2, 3), SchemaVersion.parse("1.2.3"))
        assertNull(SchemaVersion.parse("1.2"))
        assertNull(SchemaVersion.parse("v1.2.3"))
        assertNull(SchemaVersion.parse("01.2.3"))

        val current = SchemaVersion(1, 2, 3)
        assertEquals(SchemaCompat.Ok, checkSchemaCompat(SchemaVersion(1, 0, 0), current))
        assertEquals(SchemaCompat.Ok, checkSchemaCompat(SchemaVersion(1, 2, 9), current)) // patch never matters
        assertEquals(SchemaCompat.Newer(SchemaVersion(1, 3, 0)), checkSchemaCompat(SchemaVersion(1, 3, 0), current))
        assertTrue(checkSchemaCompat(SchemaVersion(2, 0, 0), current) is SchemaCompat.Unsupported)
        assertTrue(checkSchemaCompat(SchemaVersion(0, 9, 0), current) is SchemaCompat.Unsupported)
    }

    @Test
    fun themeDeclaresItsSchema() {
        val (doc, problems) = parseProblems("""{"schema":"$THEME_SCHEMA_VERSION"}""")
        assertEquals(THEME_SCHEMA_VERSION, doc?.schema)
        assertEquals(emptySet(), problems)
        assertEquals("1.0.0", parseProblems("""{"schema":1}""").first?.schema) // pre-semver files
        assertEquals(THEME_SCHEMA_VERSION, parseProblems("{}").first?.schema)
    }

    @Test
    fun otherMajorsAreRefusedAndNewerMinorsWarn() {
        val major = SchemaVersion.Current.major
        val (refused, why) = parseProblems("""{"schema":"${major + 1}.0.0"}""")
        assertNull(refused)
        assertTrue(why.single().contains("newer Spotube"))

        val (newer, warn) = parseProblems("""{"schema":"$major.${SchemaVersion.Current.minor + 1}.0"}""")
        assertNotNull(newer)
        assertTrue(warn.single().contains("newer parts are ignored"))

        assertNull(parseProblems("""{"schema":"banana"}""").first)
        assertNull(parseProblems("""{"schema":"1.0"}""").first)
    }

    @Test
    fun installKindFollowsSemver() {
        assertEquals(ThemeInstallKind.Install, classifyThemeInstall(null, "1.0.0"))
        assertEquals(ThemeInstallKind.Update, classifyThemeInstall("1.0.0", "1.0.1"))
        assertEquals(ThemeInstallKind.Update, classifyThemeInstall("1.9.0", "2.0.0"))
        assertEquals(ThemeInstallKind.Update, classifyThemeInstall("1.0.0-beta", "1.0.0"))
        assertEquals(ThemeInstallKind.SameVersion, classifyThemeInstall("1.2.3", "1.2.3"))
        assertEquals(ThemeInstallKind.Downgrade, classifyThemeInstall("2.0.0", "1.9.9"))
        assertEquals(ThemeInstallKind.InvalidIncomingVersion, classifyThemeInstall("1.0.0", "banana"))
        assertEquals(ThemeInstallKind.InvalidInstalledVersion, classifyThemeInstall("banana", "1.0.0"))
    }
}

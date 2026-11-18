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

package org.spotube.spotube.core.ui.theming

import java.io.File
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.spotube.spotube.core.ui.theming.engine.THEME_SCHEMA_VERSION
import org.spotube.spotube.core.ui.theming.engine.MODERN_JSON
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Keeps `themes/schema/theme.schema.json` (what editors and the themes repository validate against) in step
 * with [THEME_SCHEMA_VERSION]. Regenerate with `node tools/gen-theme-schema.mjs` (see the themes README).
 */
class ThemeSchemaTest {
    private val schema: JsonObject = Json.parseToJsonElement(File("../themes/schema/theme.schema.json").readText()).jsonObject

    @Test
    fun schemaFileMatchesTheVersionTheAppImplements() {
        assertEquals(THEME_SCHEMA_VERSION, schema["x-schemaVersion"]!!.jsonPrimitive.content)
        assertTrue(schema["\$id"]!!.jsonPrimitive.content.endsWith("/$THEME_SCHEMA_VERSION.json"))
    }

    @Test
    fun everyComponentTheAppKnowsIsInTheSchema() {
        val components = schema["properties"]!!.jsonObject["components"]!!.jsonObject["properties"]!!.jsonObject.keys
        val used = Json.parseToJsonElement(MODERN_JSON).jsonObject["components"]!!.jsonObject.keys
        assertTrue(components.containsAll(used), "components missing from the schema: ${used - components}")
        assertTrue(components.size >= 33)
    }
}

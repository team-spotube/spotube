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

package org.spotube.spotube.modules.plugin

import org.spotube.spotube.core.zipline.permissions.CookieDomainPattern
import com.goncalossilva.murmurhash.MurmurHash3
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonDecoder

@Serializable
enum class PluginCapability {
    @SerialName("persistent_storage")
    PERSISTENT_STORAGE,
    @SerialName("network_requests")
    NETWORK_REQUESTS,
    @SerialName("webview")
    WEBVIEW
}

//Set naming strategy to snake_case for better interoperability with JavaScript plugins
@Serializable
enum class PluginAbility {
    @SerialName("metadata")
    METADATA,
    @SerialName("audio")
    AUDIO,
    @SerialName("lyrics")
    LYRICS,
    @SerialName("scrobble")
    SCROBBLE,
}

/**
 * Decodes a list of enums while dropping values this app version doesn't know about, so a
 * plugin.json written for another plugin API revision still parses.
 */
abstract class LenientEnumListSerializer<T : Enum<T>>(
    private val elementSerializer: KSerializer<T>) : KSerializer<List<T>> {
    private val listSerializer = ListSerializer(elementSerializer)
    override val descriptor: SerialDescriptor = listSerializer.descriptor

    override fun serialize(encoder: Encoder, value: List<T>) =
        listSerializer.serialize(encoder, value)

    override fun deserialize(decoder: Decoder): List<T> {
        val jsonDecoder = decoder as? JsonDecoder ?: return listSerializer.deserialize(decoder)
        val array = jsonDecoder.decodeJsonElement() as? JsonArray
            ?: throw SerializationException("Expected a JSON array")
        return array.mapNotNull { element ->
            try {
                jsonDecoder.json.decodeFromJsonElement(elementSerializer, element)
            } catch (_: SerializationException) {
                null
            }
        }
    }
}

object LenientPluginCapabilityListSerializer :
    LenientEnumListSerializer<PluginCapability>(PluginCapability.serializer())

object LenientPluginAbilityListSerializer :
    LenientEnumListSerializer<PluginAbility>(PluginAbility.serializer())

@Serializable
data class PluginEntry(
    val name: String,
    val version: String,
    val apiVersion: String,
    val description: String,
    val author: String,
    @Serializable(with = LenientPluginCapabilityListSerializer::class)
    val capabilities: List<PluginCapability>,
    @Serializable(with = LenientPluginAbilityListSerializer::class)
    val abilities: List<PluginAbility>,
    /**
     * Hosts whose WebView cookies the plugin may read (needs [PluginCapability.WEBVIEW]):
     * `example.com` for that exact host, `*.example.com` for its subdomains (not over a public
     * suffix like `*.co.uk`). Anything else is
     * refused with an `InsufficientPermissionException`. See
     * [org.spotube.spotube.core.zipline.permissions.CookieDomainPattern].
     */
    val cookieDomains: List<String> = emptyList(),
    val contact: String,
    val repository: String,
    val bugs: String,
    val license: String) {
    @Suppress("REDUNDANT_CALL_OF_CONVERSION_METHOD")
    val id: String = MurmurHash3().hash32x86("$name:$author".encodeToByteArray())
        .toUInt()
        .toString(16)
}

/**
 * Cookie domains the user should see for [this], spelled the way the policy reads them. Only
 * meaningful with the WebView capability.
 */
val PluginEntry.displayedCookieDomains: List<String>
    get() = if (PluginCapability.WEBVIEW in capabilities) {
        cookieDomains.map(CookieDomainPattern::normalize).filter { it.isNotEmpty() }.distinct()
    } else {
        emptyList()
    }

/** Permissions an incoming plugin build asks for that the installed one didn't. */
data class PluginPermissionChanges(
    val addedCapabilities: List<PluginCapability> = emptyList(),
    val addedCookieDomains: List<String> = emptyList()) {
    val isEmpty: Boolean get() = addedCapabilities.isEmpty() && addedCookieDomains.isEmpty()
}

fun PluginEntry.permissionChangesSince(installed: PluginEntry): PluginPermissionChanges {
    val installedDomains = installed.displayedCookieDomains.toSet()
    return PluginPermissionChanges(
        addedCapabilities = capabilities.filterNot { it in installed.capabilities }.distinct(),
        addedCookieDomains = displayedCookieDomains.filterNot { it in installedDomains })
}

/** One capability as the permission dialog shows it. */
data class CapabilityPermission(val capability: PluginCapability, val isNew: Boolean)

/** One cookie domain as the permission dialog shows it. */
data class CookieDomainPermission(
    val domain: String,
    /** The parent domain for a `*.parent` wildcard, `null` for an exact host. */
    val subdomainsOf: String?,
    val isNew: Boolean)

/** A plugin's permissions, ready to render, with what's new since the installed build flagged. */
data class PluginPermissionsSummary(
    val capabilities: List<CapabilityPermission>,
    val cookieDomains: List<CookieDomainPermission>)

fun PluginEntry.permissionsSummary(
    changes: PluginPermissionChanges = PluginPermissionChanges()): PluginPermissionsSummary {
    val newDomains = changes.addedCookieDomains.toSet()
    return PluginPermissionsSummary(
        capabilities = capabilities.distinct().map { CapabilityPermission(it, it in changes.addedCapabilities) },
        cookieDomains = displayedCookieDomains.map { domain ->
            CookieDomainPermission(
                domain = domain,
                subdomainsOf = domain.takeIf { it.startsWith("*.") }?.removePrefix("*."),
                isNew = domain in newDomains)
        })
}

sealed class PluginManagerStates {
    @Serializable
    data class Data(
        val plugins: List<PluginEntry>,
        val selectedPlugins: Map<PluginAbility, PluginEntry> = emptyMap(),
        // Bumped on every addPlugin so the DataStore always detects a structural
        // change — without it, same-version replaces produce identical JSON and
        // the DataStore's internal distinctUntilChanged blocks the state emission,
        // leaving the ziplineServices flow stuck with the old code.
        val generation: Long = 0L
    ) : PluginManagerStates()

    data object Loading : PluginManagerStates()
//    data class Error(val message: String) : PluginViewModelStates()
}
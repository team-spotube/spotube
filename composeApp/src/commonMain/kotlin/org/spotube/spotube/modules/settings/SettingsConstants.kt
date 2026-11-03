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

package org.spotube.spotube.modules.settings

import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

@Serializable
enum class CountryCode(val code: String, val displayName: String) {
    AD("AD", "Andorra"),
    AE("AE", "United Arab Emirates"),
    AF("AF", "Afghanistan"),
    AG("AG", "Antigua and Barbuda"),
    AI("AI", "Anguilla"),
    AL("AL", "Albania"),
    AM("AM", "Armenia"),
    AO("AO", "Angola"),
    AQ("AQ", "Antarctica"),
    AR("AR", "Argentina"),
    AS("AS", "American Samoa"),
    AT("AT", "Austria"),
    AU("AU", "Australia"),
    AW("AW", "Aruba"),
    AX("AX", "Aland Islands"),
    AZ("AZ", "Azerbaijan"),
    BA("BA", "Bosnia and Herzegovina"),
    BB("BB", "Barbados"),
    BD("BD", "Bangladesh"),
    BE("BE", "Belgium"),
    BF("BF", "Burkina Faso"),
    BG("BG", "Bulgaria"),
    BH("BH", "Bahrain"),
    BI("BI", "Burundi"),
    BJ("BJ", "Benin"),
    BL("BL", "Saint Barthelemy"),
    BM("BM", "Bermuda"),
    BN("BN", "Brunei Darussalam"),
    BO("BO", "Bolivia"),
    BQ("BQ", "Bonaire, Sint Eustatius and Saba"),
    BR("BR", "Brazil"),
    BS("BS", "Bahamas"),
    BT("BT", "Bhutan"),
    BV("BV", "Bouvet Island"),
    BW("BW", "Botswana"),
    BY("BY", "Belarus"),
    BZ("BZ", "Belize"),
    CA("CA", "Canada"),
    CC("CC", "Cocos (Keeling) Islands"),
    CD("CD", "Democratic Republic of the Congo"),
    CF("CF", "Central African Republic"),
    CG("CG", "Congo"),
    CH("CH", "Switzerland"),
    CI("CI", "Cote d'Ivoire"),
    CK("CK", "Cook Islands"),
    CL("CL", "Chile"),
    CM("CM", "Cameroon"),
    CN("CN", "China"),
    CO("CO", "Colombia"),
    CR("CR", "Costa Rica"),
    CU("CU", "Cuba"),
    CV("CV", "Cabo Verde"),
    CW("CW", "Curacao"),
    CX("CX", "Christmas Island"),
    CY("CY", "Cyprus"),
    CZ("CZ", "Czechia"),
    DE("DE", "Germany"),
    DJ("DJ", "Djibouti"),
    DK("DK", "Denmark"),
    DM("DM", "Dominica"),
    DO("DO", "Dominican Republic"),
    DZ("DZ", "Algeria"),
    EC("EC", "Ecuador"),
    EE("EE", "Estonia"),
    EG("EG", "Egypt"),
    EH("EH", "Western Sahara"),
    ER("ER", "Eritrea"),
    ES("ES", "Spain"),
    ET("ET", "Ethiopia"),
    FI("FI", "Finland"),
    FJ("FJ", "Fiji"),
    FK("FK", "Falkland Islands"),
    FM("FM", "Micronesia"),
    FO("FO", "Faroe Islands"),
    FR("FR", "France"),
    GA("GA", "Gabon"),
    GB("GB", "United Kingdom"),
    GD("GD", "Grenada"),
    GE("GE", "Georgia"),
    GF("GF", "French Guiana"),
    GG("GG", "Guernsey"),
    GH("GH", "Ghana"),
    GI("GI", "Gibraltar"),
    GL("GL", "Greenland"),
    GM("GM", "Gambia"),
    GN("GN", "Guinea"),
    GP("GP", "Guadeloupe"),
    GQ("GQ", "Equatorial Guinea"),
    GR("GR", "Greece"),
    GS("GS", "South Georgia and the South Sandwich Islands"),
    GT("GT", "Guatemala"),
    GU("GU", "Guam"),
    GW("GW", "Guinea-Bissau"),
    GY("GY", "Guyana"),
    HK("HK", "Hong Kong"),
    HM("HM", "Heard Island and McDonald Islands"),
    HN("HN", "Honduras"),
    HR("HR", "Croatia"),
    HT("HT", "Haiti"),
    HU("HU", "Hungary"),
    ID("ID", "Indonesia"),
    IE("IE", "Ireland"),
    IL("IL", "Israel"),
    IM("IM", "Isle of Man"),
    IN("IN", "India"),
    IO("IO", "British Indian Ocean Territory"),
    IQ("IQ", "Iraq"),
    IR("IR", "Iran"),
    IS("IS", "Iceland"),
    IT("IT", "Italy"),
    JE("JE", "Jersey"),
    JM("JM", "Jamaica"),
    JO("JO", "Jordan"),
    JP("JP", "Japan"),
    KE("KE", "Kenya"),
    KG("KG", "Kyrgyzstan"),
    KH("KH", "Cambodia"),
    KI("KI", "Kiribati"),
    KM("KM", "Comoros"),
    KN("KN", "Saint Kitts and Nevis"),
    KP("KP", "North Korea"),
    KR("KR", "South Korea"),
    KW("KW", "Kuwait"),
    KY("KY", "Cayman Islands"),
    KZ("KZ", "Kazakhstan"),
    LA("LA", "Lao People's Democratic Republic"),
    LB("LB", "Lebanon"),
    LC("LC", "Saint Lucia"),
    LI("LI", "Liechtenstein"),
    LK("LK", "Sri Lanka"),
    LR("LR", "Liberia"),
    LS("LS", "Lesotho"),
    LT("LT", "Lithuania"),
    LU("LU", "Luxembourg"),
    LV("LV", "Latvia"),
    LY("LY", "Libya"),
    MA("MA", "Morocco"),
    MC("MC", "Monaco"),
    MD("MD", "Moldova"),
    ME("ME", "Montenegro"),
    MF("MF", "Saint Martin (French part)"),
    MG("MG", "Madagascar"),
    MH("MH", "Marshall Islands"),
    MK("MK", "North Macedonia"),
    ML("ML", "Mali"),
    MM("MM", "Myanmar"),
    MN("MN", "Mongolia"),
    MO("MO", "Macao"),
    MP("MP", "Northern Mariana Islands"),
    MQ("MQ", "Martinique"),
    MR("MR", "Mauritania"),
    MS("MS", "Montserrat"),
    MT("MT", "Malta"),
    MU("MU", "Mauritius"),
    MV("MV", "Maldives"),
    MW("MW", "Malawi"),
    MX("MX", "Mexico"),
    MY("MY", "Malaysia"),
    MZ("MZ", "Mozambique"),
    NA("NA", "Namibia"),
    NC("NC", "New Caledonia"),
    NE("NE", "Niger"),
    NF("NF", "Norfolk Island"),
    NG("NG", "Nigeria"),
    NI("NI", "Nicaragua"),
    NL("NL", "Netherlands"),
    NO("NO", "Norway"),
    NP("NP", "Nepal"),
    NR("NR", "Nauru"),
    NU("NU", "Niue"),
    NZ("NZ", "New Zealand"),
    OM("OM", "Oman"),
    PA("PA", "Panama"),
    PE("PE", "Peru"),
    PF("PF", "French Polynesia"),
    PG("PG", "Papua New Guinea"),
    PH("PH", "Philippines"),
    PK("PK", "Pakistan"),
    PL("PL", "Poland"),
    PM("PM", "Saint Pierre and Miquelon"),
    PN("PN", "Pitcairn"),
    PR("PR", "Puerto Rico"),
    PS("PS", "Palestine, State of"),
    PT("PT", "Portugal"),
    PW("PW", "Palau"),
    PY("PY", "Paraguay"),
    QA("QA", "Qatar"),
    RE("RE", "Reunion"),
    RO("RO", "Romania"),
    RS("RS", "Serbia"),
    RU("RU", "Russian Federation"),
    RW("RW", "Rwanda"),
    SA("SA", "Saudi Arabia"),
    SB("SB", "Solomon Islands"),
    SC("SC", "Seychelles"),
    SD("SD", "Sudan"),
    SE("SE", "Sweden"),
    SG("SG", "Singapore"),
    SH("SH", "Saint Helena, Ascension and Tristan da Cunha"),
    SI("SI", "Slovenia"),
    SJ("SJ", "Svalbard and Jan Mayen"),
    SK("SK", "Slovakia"),
    SL("SL", "Sierra Leone"),
    SM("SM", "San Marino"),
    SN("SN", "Senegal"),
    SO("SO", "Somalia"),
    SR("SR", "Suriname"),
    SS("SS", "South Sudan"),
    ST("ST", "Sao Tome and Principe"),
    SV("SV", "El Salvador"),
    SX("SX", "Sint Maarten (Dutch part)"),
    SY("SY", "Syrian Arab Republic"),
    SZ("SZ", "Eswatini"),
    TC("TC", "Turks and Caicos Islands"),
    TD("TD", "Chad"),
    TF("TF", "French Southern Territories"),
    TG("TG", "Togo"),
    TH("TH", "Thailand"),
    TJ("TJ", "Tajikistan"),
    TK("TK", "Tokelau"),
    TL("TL", "Timor-Leste"),
    TM("TM", "Turkmenistan"),
    TN("TN", "Tunisia"),
    TO("TO", "Tonga"),
    TR("TR", "Turkey"),
    TT("TT", "Trinidad and Tobago"),
    TV("TV", "Tuvalu"),
    TW("TW", "Taiwan"),
    TZ("TZ", "Tanzania"),
    UA("UA", "Ukraine"),
    UG("UG", "Uganda"),
    UM("UM", "United States Minor Outlying Islands"),
    US("US", "United States"),
    UY("UY", "Uruguay"),
    UZ("UZ", "Uzbekistan"),
    VA("VA", "Holy See"),
    VC("VC", "Saint Vincent and the Grenadines"),
    VE("VE", "Venezuela"),
    VG("VG", "Virgin Islands (British)"),
    VI("VI", "Virgin Islands (U.S.)"),
    VN("VN", "Viet Nam"),
    VU("VU", "Vanuatu"),
    WF("WF", "Wallis and Futuna"),
    WS("WS", "Samoa"),
    YE("YE", "Yemen"),
    YT("YT", "Mayotte"),
    ZA("ZA", "South Africa"),
    ZM("ZM", "Zambia"),
    ZW("ZW", "Zimbabwe");

    companion object {
        fun fromCode(code: String): CountryCode? {
            return entries.find { it.code == code.uppercase() }
        }
    }
}

/**
 * App UI languages. Each one (except [SYSTEM]) must have a matching
 * `composeResources/values-<qualifier>/strings.xml`. [displayName] is the language's own name
 * (endonym), so it is intentionally not localized.
 */
@Serializable
enum class SupportedLanguages(
    val locale: String,
    val country: String,
    val displayName: String,
    val isRtl: Boolean = false
) {
    /** Follow the device/OS language. */
    SYSTEM("", "", ""),
    EN("en", "", "English"),
    AR("ar", "", "العربية", isRtl = true),
    BN("bn", "", "বাংলা"),
    DE("de", "", "Deutsch"),
    ES("es", "", "Español"),
    FA("fa", "", "فارسی", isRtl = true),
    FR("fr", "", "Français"),
    HI("hi", "", "हिन्दी"),
    ID("id", "", "Bahasa Indonesia"),
    IT("it", "", "Italiano"),
    JA("ja", "", "日本語"),
    KO("ko", "", "한국어"),
    NL("nl", "", "Nederlands"),
    PL("pl", "", "Polski"),
    PT_BR("pt", "BR", "Português (Brasil)"),
    RU("ru", "", "Русский"),
    TH("th", "", "ไทย"),
    TR("tr", "", "Türkçe"),
    UK("uk", "", "Українська"),
    VI("vi", "", "Tiếng Việt"),
    ZH_CN("zh", "CN", "简体中文"),
    ZH_TW("zh", "TW", "繁體中文");

    /** BCP 47 tag (e.g. "zh-TW"), or null for [SYSTEM]. */
    val languageTag: String?
        get() = when {
            this == SYSTEM -> null
            country.isEmpty() -> locale
            else -> "$locale-$country"
        }
}

@Serializable
enum class AccentColors(val lightHex: Long, val darkHex: Long) {
    // High-contrast Forest / Neon Mint
    GREEN_GOBLIN(0xFF006D3A, 0xFF80DB92),

    // Royal Purple / Soft Lavender
    ELECTRIC_VIOLET(0xFF6C40BF, 0xFFD0BCFF),

    // Deep Teal / Icy Cyan
    OCEANIC_CYAN(0xFF00677D, 0xFF59D3ED),

    // Burnt Sienna / Peach Glow
    SUNSET_ORANGE(0xFFA23F16, 0xFFFFB596),

    // Crimson Red / Pastel Pink-Red
    ROSE_GARDEN(0xFFB12E49, 0xFFFFB2BB),

    // Deep Cobalt / Sky Blue
    MIDNIGHT_BLUE(0xFF0056D2, 0xFFB1C5FF),

    // Slate Gray / Silver
    METALLIC_SLATE(0xFF5A5F6B, 0xFFC2C7D4);

    companion object {
        fun fromHex(lightHex: Long, darkHex: Long): AccentColors? {
            return entries.find { it.lightHex == lightHex && it.darkHex == darkHex }
        }
    }

    fun toLightColor() = Color(lightHex)
    fun toDarkColor() = Color(darkHex)
}
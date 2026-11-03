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

package org.spotube.spotube.core.discovery

/**
 * The LAN address to publish in our mDNS advertisement, or null to let the platform decide.
 *
 * Only the desktop needs this: there the dns-sd library otherwise grabs the first site-local
 * address of *any* interface, which on a dev box is often a Docker/VM bridge (e.g. minikube's
 * 192.168.49.1) that other devices can't reach.
 */
internal expect fun preferredAdvertiseAddress(): String?

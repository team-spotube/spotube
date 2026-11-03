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

package org.spotube.plugin_interfaces.core.browser_apis

fun fastTextEncodingPolyfill() {
    js(
        """
        (function(scope) {'use strict';
        function B(r,e){var f;return r instanceof Buffer?f=r:f=Buffer.from(r.buffer,r.byteOffset,r.byteLength),f.toString(e)}var w=function(r){return Buffer.from(r)};function h(r){for(var e=0,f=Math.min(256*256,r.length+1),n=new Uint16Array(f),i=[],o=0;;){var t=e<r.length;if(!t||o>=f-1){var s=n.subarray(0,o),m=s;if(i.push(String.fromCharCode.apply(null,m)),!t)return i.join("");r=r.subarray(e),e=0,o=0}var a=r[e++];if((a&128)===0)n[o++]=a;else if((a&224)===192){var d=r[e++]&63;n[o++]=(a&31)<<6|d}else if((a&240)===224){var d=r[e++]&63,l=r[e++]&63;n[o++]=(a&31)<<12|d<<6|l}else if((a&248)===240){var d=r[e++]&63,l=r[e++]&63,R=r[e++]&63,c=(a&7)<<18|d<<12|l<<6|R;c>65535&&(c-=65536,n[o++]=c>>>10&1023|55296,c=56320|c&1023),n[o++]=c}}}function F(r){for(var e=0,f=r.length,n=0,i=Math.max(32,f+(f>>>1)+7),o=new Uint8Array(i>>>3<<3);e<f;){var t=r.charCodeAt(e++);if(t>=55296&&t<=56319){if(e<f){var s=r.charCodeAt(e);(s&64512)===56320&&(++e,t=((t&1023)<<10)+(s&1023)+65536)}if(t>=55296&&t<=56319)continue}if(n+4>o.length){i+=8,i*=1+e/r.length*2,i=i>>>3<<3;var m=new Uint8Array(i);m.set(o),o=m}if((t&4294967168)===0){o[n++]=t;continue}else if((t&4294965248)===0)o[n++]=t>>>6&31|192;else if((t&4294901760)===0)o[n++]=t>>>12&15|224,o[n++]=t>>>6&63|128;else if((t&4292870144)===0)o[n++]=t>>>18&7|240,o[n++]=t>>>12&63|128,o[n++]=t>>>6&63|128;else continue;o[n++]=t&63|128}return o.slice?o.slice(0,n):o.subarray(0,n)}var u="Failed to ",p=function(r,e,f){if(r)throw new Error("".concat(u).concat(e,": the '").concat(f,"' option is unsupported."))};var x=typeof Buffer=="function"&&Buffer.from;var A=x?w:F;function v(){this.encoding="utf-8"}v.prototype.encode=function(r,e){return p(e&&e.stream,"encode","stream"),A(r)};function U(r){var e;try{var f=new Blob([r],{type:"text/plain;charset=UTF-8"});e=URL.createObjectURL(f);var n=new XMLHttpRequest;return n.open("GET",e,!1),n.send(),n.responseText}finally{e&&URL.revokeObjectURL(e)}}var O=!x&&typeof Blob=="function"&&typeof URL=="function"&&typeof URL.createObjectURL=="function",S=["utf-8","utf8","unicode-1-1-utf-8"],T=h;x?T=B:O&&(T=function(r){try{return U(r)}catch(e){return h(r)}});var y="construct 'TextDecoder'",E="".concat(u," ").concat(y,": the ");function g(r,e){p(e&&e.fatal,y,"fatal"),r=r||"utf-8";var f;if(x?f=Buffer.isEncoding(r):f=S.indexOf(r.toLowerCase())!==-1,!f)throw new RangeError("".concat(E," encoding label provided ('").concat(r,"') is invalid."));this.encoding=r,this.fatal=!1,this.ignoreBOM=!1}g.prototype.decode=function(r,e){p(e&&e.stream,"decode","stream");var f;return r instanceof Uint8Array?f=r:r.buffer instanceof ArrayBuffer?f=new Uint8Array(r.buffer):f=new Uint8Array(r),T(f,this.encoding)};scope.TextEncoder=scope.TextEncoder||v;scope.TextDecoder=scope.TextDecoder||g;
        }(typeof window !== 'undefined' ? window : (typeof globalThis !== 'undefined' ? globalThis : this)));
    """
    )
}
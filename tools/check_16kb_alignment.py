#!/usr/bin/env python3
"""Comprueba que las librerías nativas de un APK cumplen el requisito de páginas de 16 KB de Android.

Google Play lo exige desde noviembre de 2025 para apps con targetSdk >= 35. Hacen falta dos cosas, y el
diálogo de aviso del dispositivo no distingue bien entre ellas:

  1. Cada `.so` debe tener sus segmentos LOAD alineados a 16 KB (0x4000). Esto depende de con qué flags
     compiló el proveedor de la librería: no se puede arreglar desde nuestro build, solo subiendo versión.
  2. Dentro del APK, cada `.so` debe ir sin comprimir (STORED) y empezar en un offset múltiplo de 16 KB,
     para que el loader pueda mapearlo directamente. De esto se encarga AGP.

Uso:
    python3 tools/check_16kb_alignment.py [ruta/al.apk]

Sin argumento usa el APK de debug. Devuelve código 1 si algo incumple, para poder encadenarlo en CI.
"""

from __future__ import annotations

import struct
import sys
import zipfile
from pathlib import Path

PAGE_SIZE = 16 * 1024
PT_LOAD = 1
DEFAULT_APK = Path("androidApp/build/intermediates/apk/debug/androidApp-debug.apk")


def load_segment_alignments(elf: bytes) -> list[int]:
    """Devuelve la alineación de cada segmento LOAD de un ELF de 64 bits."""
    program_header_offset = struct.unpack_from("<Q", elf, 0x20)[0]
    entry_size = struct.unpack_from("<H", elf, 0x36)[0]
    entry_count = struct.unpack_from("<H", elf, 0x38)[0]

    alignments = []
    for index in range(entry_count):
        entry = program_header_offset + index * entry_size
        if struct.unpack_from("<I", elf, entry)[0] == PT_LOAD:
            alignments.append(struct.unpack_from("<Q", elf, entry + 48)[0])
    return alignments


def data_offset(apk: Path, entry: zipfile.ZipInfo) -> int:
    """Offset real de los bytes del fichero dentro del zip (la cabecera local es de tamaño variable)."""
    with apk.open("rb") as handle:
        handle.seek(entry.header_offset + 26)
        name_length, extra_length = struct.unpack("<HH", handle.read(4))
    return entry.header_offset + 30 + name_length + extra_length


def check(apk: Path) -> bool:
    archive = zipfile.ZipFile(apk)
    libraries = [e for e in archive.infolist() if e.filename.endswith(".so")]
    if not libraries:
        print(f"Sin librerías nativas en {apk}")
        return True

    all_ok = True
    for entry in libraries:
        # Solo 64 bits: las ABIs de 32 bits no están sujetas al requisito.
        if "arm64" not in entry.filename and "x86_64" not in entry.filename:
            continue

        alignments = load_segment_alignments(archive.read(entry))
        stored = entry.compress_type == zipfile.ZIP_STORED
        offset = data_offset(apk, entry)

        problems = []
        if not all(a >= PAGE_SIZE for a in alignments):
            problems.append(f"segmentos LOAD a {[hex(a) for a in alignments]}")
        if not stored:
            problems.append("comprimido en el APK")
        if offset % PAGE_SIZE:
            problems.append(f"offset {offset:#x} no alineado")

        all_ok &= not problems
        status = "OK " if not problems else "MAL"
        detail = "" if not problems else "  <- " + "; ".join(problems)
        print(f"{status} {entry.filename}{detail}")

    return all_ok


if __name__ == "__main__":
    target = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_APK
    if not target.is_file():
        sys.exit(f"No existe el APK: {target}")
    print(f"Analizando {target}\n")
    sys.exit(0 if check(target) else 1)

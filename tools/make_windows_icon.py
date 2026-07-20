#!/usr/bin/env python3
"""Genera el `.ico` de Windows a partir del PNG maestro del icono.

Windows necesita un `.ico`, que es un contenedor con la misma imagen a varios tamaños; el sistema elige el
que le conviene según dónde la pinte (barra de tareas, escritorio, Alt+Tab). Desde Vista el formato admite
que cada entrada sea un PNG embebido tal cual, así que basta con escalar con `sips` y concatenar: no hace
falta ImageMagick ni Pillow, que no están instalados en el equipo de desarrollo.

Uso:
    python3 tools/make_windows_icon.py [origen.png] [destino.ico]

Por defecto toma `desktopApp/icons/citovision-1024.png` y escribe `desktopApp/icons/citovision.ico`.
"""

from __future__ import annotations

import struct
import subprocess
import sys
import tempfile
from pathlib import Path

# Tamaños que Windows usa habitualmente. 256 es el máximo que admite el formato en una entrada y el que se
# ve en vistas grandes del explorador; los pequeños se generan aparte porque reducir 256 -> 16 al vuelo da
# un resultado peor que escalar desde el maestro.
SIZES = (16, 24, 32, 48, 64, 128, 256)

REPO_ROOT = Path(__file__).resolve().parent.parent
DEFAULT_SOURCE = REPO_ROOT / "desktopApp" / "icons" / "citovision-1024.png"
DEFAULT_TARGET = REPO_ROOT / "desktopApp" / "icons" / "citovision.ico"

ICONDIR = "<HHH"
ICONDIRENTRY = "<BBBBHHII"


def scale(source: Path, size: int, directory: Path) -> bytes:
    """Escala el maestro a `size` con `sips` y devuelve el PNG resultante."""
    output = directory / f"icon_{size}.png"
    subprocess.run(
        ["sips", "-z", str(size), str(size), str(source), "--out", str(output)],
        check=True,
        capture_output=True,
    )
    return output.read_bytes()


def build_ico(images: list[tuple[int, bytes]]) -> bytes:
    """Ensambla las entradas en un `.ico`: cabecera, tabla de entradas y los PNG en bruto detrás."""
    header = struct.pack(ICONDIR, 0, 1, len(images))  # reservado, tipo 1 = icono, número de imágenes
    offset = len(header) + len(images) * struct.calcsize(ICONDIRENTRY)
    entries = bytearray()
    payload = bytearray()
    for size, data in images:
        # 256 se codifica como 0: el campo es de un byte y no llega a 256.
        dimension = 0 if size >= 256 else size
        entries += struct.pack(ICONDIRENTRY, dimension, dimension, 0, 0, 1, 32, len(data), offset)
        payload += data
        offset += len(data)
    return bytes(header + entries + payload)


def main() -> int:
    source = Path(sys.argv[1]) if len(sys.argv) > 1 else DEFAULT_SOURCE
    target = Path(sys.argv[2]) if len(sys.argv) > 2 else DEFAULT_TARGET

    if not source.is_file():
        print(f"No existe el PNG de origen: {source}", file=sys.stderr)
        return 1

    with tempfile.TemporaryDirectory() as raw_directory:
        directory = Path(raw_directory)
        images = [(size, scale(source, size, directory)) for size in SIZES]

    target.write_bytes(build_ico(images))
    total = target.stat().st_size
    print(f"Escrito {target} ({total} bytes) con {len(images)} tamaños: {', '.join(map(str, SIZES))}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())

# DESIGN.md

## Product Identity

### Product Name

citoVision

### Design Philosophy


Ejemplos:

- Simple y minimalista.
- Profesional y corporativa.
- Moderna y amigable.
- Orientada a productividad.
- Orientada a uso de profesionales del sector médico.
- Material 3.

### Design Keywords


design_keywords:

  - professional
  - trustworthy
  - conservative
  - clean
  - accessible
  - medical

When generating a screen, it should aim to:

- avoid bright or overly vibrant colors
- avoid flashy animations; all animations should be subtle, smooth, and calm
- use classic, familiar layouts
- prioritize readability and clarity

---

# Target Platforms

- Android
- iOS
- Desktop
- Ensure proper support for Android tablets and iPads.

---

# Color System

## Primary

```yaml
primary: "#4E54EA"        # Azul violeta principal (ej. botón sin pulsar)
primaryPressed: "#292FD5" # Azul violeta pulsado
onPrimary: "#FFFFFF"      # Texto sobre azul violeta
```

## Secondary

```yaml
secondary: "#252466"        # Azul marino (ej. botón Analizar, indicador de pestaña activa)
secondaryPressed: "#171643" # Azul marino pulsado
secondaryDark: "#177552"    # Verde oscuro: texto/etiquetas de la prioridad BAJA sobre fondo claro. NO deriva de `secondary`; es un color semántico del semáforo de prioridad y por eso permanece verde
onSecondary: "#FFFFFF"      # Texto sobre azul marino
```

## Tertiary

```yaml
tertiary: "#252466"   # Azul marino: nombre de la aplicación, resplandor de fondo, avatar de inicial
onTertiary: "#FFFFFF" # Texto sobre azul marino
```

## Background

```yaml
background: "#FFFFFF" # Fondo principal
surface: "#FFFFFF"    # Superficie
onBackground: "#282828" # Texto principal
onSurface: "#6F6F6F"    # Texto secundario/aclaratorio
hint: "#9E9E9E"         # Texto suave: placeholders/hints de campos de texto (tercer nivel, más tenue que onSurface)
```
> Nota de accesibilidad: `hint` (#9E9E9E) tiene un contraste ~2.9:1 sobre blanco, por debajo de WCAG AA (4.5:1). Se admite únicamente para el texto de *placeholder* de los campos, que no es contenido esencial (cada campo lleva su label visible encima) y desaparece al escribir. No usar `hint` para contenido de lectura.

## Semantic Colors

```yaml
success: "#2FD38A"      # Verde de éxito
warning: "#F59E3A"      # Naranja proporcionado
error: "#F53A63"        # Rojo principal
errorPressed: "#C71C43" # Rojo de card pulsado
info: "#2FA7F0"         # Azul informativo
```
- Nota sobre el color: el nombre de la aplicación, siempre que aparezca, irá en el color terciario.
- Los colores semánticos son **independientes de la marca**: describen un estado, no la identidad. En particular, el semáforo de prioridad (verde BAJA → ámbar MEDIA → rojo ALTA) no se rebrandea, porque su código cromático es lo que comunica el resultado del análisis.
---

# Elevation

```yaml
  none: 0
  low: 2
  medium: 6
  high: 12
```

---

# Typography

## Font Family

```yaml
fontFamily: Dongle (regular and bold)
```

## Scale

```yaml
  displayLarge: 72sp
  headlineLarge: 56sp
  headlineMedium: 48sp
  titleLarge: 40sp
  bodyLarge: 32sp
  bodyMedium: 28sp
  labelLarge: 28sp
```

---

# Spacing System

```yaml
baseUnit: 4dp
```

```yaml
spacing:
  xs: 4
  sm: 8
  md: 16
  lg: 24
  xl: 32
  xxl: 48
  xxxl: 64
  ```

---

# Corner Radius

```yaml
small: 12dp
medium: 16dp
large: 20dp
extraLarge: 28dp
```

---

# Components

## Buttons


### Primary Button

- Fondo: Primary
- Texto: OnPrimary
- Radio: medium

### Secondary Button

- Outline
- Outline color: Primary
- Texto: Primary
- Fondo transparente

### Destructive Button

- Color Error solid
- Texto: onPrimary


## Cards

### Standard Card
- Fondo: surface (#FFFFFF).
- Radio: medium.
- Elevation: elevation (12dp)
- Texto Título: onBackground (#282828).
- Texto Cuerpo: onSurface (#6F6F6F).
- Incluye primary button

### Error Card
- Fondo: surface (#FFFFFF).
- Elevation: elevation (12dp)
- Elevation Color: error
- Radio: medium.
- Incluye error button

---

# Navigation

## Pattern

- Bottom Navigation
- Navigation Rail
- Tabs

---

# Screen States

- Loading
- Success
- Empty
- Error

---

# Mobile Specific Rules

## Touch Targets

```yaml
minimumTouchTarget: 48dp
```

## Android

- Edge-to-edge enabled.
- Material 3.
- Soporte modo oscuro.

## iOS

- Respetar Safe Areas.
- Mantener comportamiento nativo cuando sea posible.

---

# Dark Mode

```yaml
enabled: true
selection: user            # Elegible por el usuario en Ajustes
options: [light, dark, system]
default: system            # "Seguir sistema" por defecto
```

El usuario elige el tema en Ajustes entre tres opciones excluyentes: **Claro**, **Oscuro** o
**Seguir sistema** (usa `isSystemInDarkTheme()`). La preferencia se persiste (DataStore).

## Paleta oscura

Misma identidad de marca que el tema claro, adaptada a fondo oscuro: fondos/textos invertidos (nunca negro
puro), colores de marca **aclarados** hasta un tono intermedio, y `secondaryDark` **invertido** a
un verde claro (en claro es verde oscuro para texto sobre blanco; en oscuro, verde claro para texto sobre
fondo oscuro). El fondo lleva un ligero tinte azulado de marca.

**Por qué un tono intermedio y no el color de marca tal cual.** `primary`, `secondary` y `tertiary` se usan
de dos formas opuestas: como **relleno** con texto blanco encima (botones, avatar de inicial) y como
**texto** sobre el fondo (nombre de la aplicación, mensajes de confirmación). Sobre `#111318` ningún color
puede cumplir AA en ambos papeles a la vez: leerse como texto exige una luminancia relativa ≥ 0,204 y que el
blanco encima cumpla AA exige ≤ 0,183, y esos rangos no se solapan. Los valores de abajo son el compromiso:
**AA como relleno y AA de texto grande como texto**, que es el criterio aplicable porque el nombre de la
aplicación se pinta a 88sp y los mensajes a 28sp.

```yaml
# Primary
primary: "#7378EE"        # Azul violeta principal (aclarado)
primaryPressed: "#5F63CF" # Azul violeta pulsado
onPrimary: "#FFFFFF"      # Texto sobre azul violeta

# Secondary
secondary: "#6362D0"        # Azul marino (aclarado)
secondaryPressed: "#4B4AA6" # Azul marino pulsado
secondaryDark: "#64D3A1"    # Verde claro: texto/etiquetas de la prioridad BAJA sobre fondo oscuro. NO deriva de `secondary`; es semántico y por eso permanece verde
onSecondary: "#FFFFFF"      # Texto sobre azul marino

# Tertiary
tertiary: "#6362D0"   # Azul marino (aclarado)
onTertiary: "#FFFFFF" # Texto sobre azul marino

# Background
background: "#111318" # Fondo principal (oscuro con tinte azulado)
surface: "#262A33"    # Superficie (claramente más clara que el fondo, para diferenciar las cards)
onBackground: "#ECECEC" # Texto principal
onSurface: "#A8A8A8"    # Texto secundario/aclaratorio
hint: "#707070"         # Texto suave: placeholders/hints (más tenue que onSurface)

# Semantic
success: "#3EC589"      # Verde de éxito
warning: "#E59D4B"      # Naranja (desaturado)
error: "#E14869"        # Rojo principal (desaturado)
errorPressed: "#B52949" # Rojo pulsado
info: "#42A4E0"         # Azul informativo
```

---

# Accessibility

- Contraste WCAG AA.
- Dynamic Type.
- Screen Readers.

---

# AI Instructions

1. Utilizar únicamente colores definidos en este documento.
2. Utilizar únicamente radios definidos en este documento.
3. Utilizar únicamente spacing definido en este documento.
4. Reutilizar componentes existentes antes de crear nuevos.
5. Mantener coherencia visual con pantallas ya implementadas.
6. Mantener paridad visual entre Android e iOS.

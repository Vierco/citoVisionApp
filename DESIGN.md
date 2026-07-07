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
primary: "#2FA7F0"        # Azul principal (ej. botón sin pulsar)
primaryPressed: "#227FC0" # Azul pulsado
onPrimary: "#FFFFFF"      # Texto sobre azul
```

## Secondary

```yaml
secondary: "#2FD38A"        # Verde (ej. botón secundario sin pulsar)
secondaryPressed: "#1E9E67" # Verde pulsado
onSecondary: "#FFFFFF"      # Texto sobre verde
```

## Tertiary

```yaml
tertiary: "#A56AE3"   # Morado (ej. outline de cards)
onTertiary: "#FFFFFF" # Texto sobre morado
```

## Background

```yaml
background: "#FFFFFF" # Fondo principal
surface: "#FFFFFF"    # Superficie
onBackground: "#282828" # Texto principal
onSurface: "#6F6F6F"    # Texto secundario/aclaratorio
```

## Semantic Colors

```yaml
success: "#2FD38A"      # Verde (coincide con secondary)
warning: "#F59E3A"      # Naranja proporcionado
error: "#F53A63"        # Rojo principal
errorPressed: "#C71C43" # Rojo de card pulsado
info: "#2FA7F0"         # Azul (coincide con primary)
```
- Nota sobre el color: el nombre de la aplicación, siempre que aparezca, irá en el color terciario.
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
enabled: false
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

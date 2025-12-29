# Regex Engine UI

A beautiful, responsive web interface for the Regex Engine API built with Stencil.js.

## Features

- **Match** - Full string pattern matching
- **Find** - Search for patterns in text
- **Replace** - Find and replace all occurrences
- **Split** - Split text by pattern
- **Benchmark** - Compare all three engines (Backtracking, NFA, DFA)
- **Monitor** - Real-time health and performance metrics

## Tech Stack

- **Stencil.js** - Web component compiler
- **TypeScript** - Type-safe JavaScript
- **CSS3** - Modern styling with CSS variables
- **JetBrains Mono + Space Grotesk** - Typography

## Design

The UI features a modern, dark theme inspired by terminal aesthetics:

- Dark luxury color palette with neon accents (#00ff88, #00ccff, #ff6b9d)
- Smooth animations and micro-interactions
- Responsive design for mobile and desktop
- Monospace code inputs with syntax highlighting
- Real-time result highlighting

## Getting Started

### Prerequisites

- Node.js 18+
- npm or yarn

### Installation

```bash
cd frontend
npm install
```

### Development

```bash
npm start
```

This starts the Stencil development server at `http://localhost:3333`.

### Build

```bash
npm run build
```

The built files will be in `www/` and can be served statically.

### Integration with Spring Boot

Copy the built files to Spring Boot's static resources:

```bash
cp -r www/* ../src/main/resources/static/
```

Then access the UI at `http://localhost:8080/api/v1/re` when running the Spring Boot app.

## Components

| Component           | Description                              |
|---------------------|------------------------------------------|
| `<regex-app>`       | Main application shell with navigation   |
| `<regex-docs>`      | Documentation and Links                  |
| `<regex-matcher>`   | Match/Find operations form and results   |
| `<regex-replace>`   | Replace operation with before/after view |
| `<regex-split>`     | Split operation with parts display       |
| `<regex-benchmark>` | Engine comparison with timing bars       |
| `<regex-monitor>`   | Health metrics dashboard                 |

## API Endpoints Used

- `POST /regex/match` - Full match
- `POST /regex/find` - Find first
- `POST /regex/find-all` - Find all
- `POST /regex/replace` - Replace all
- `POST /regex/split` - Split by pattern
- `POST /regex/benchmark` - Engine benchmark
- `GET /monitor/status` - Health status
- `GET /monitor/metrics` - Detailed metrics

## Browser Support

- Chrome 80+
- Firefox 75+
- Safari 14+
- Edge 80+

## License

MIT

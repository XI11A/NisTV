# NisTV Revamped — Official Web Portal & APK Download Center

A modern, fast, responsive landing page and download portal for the **NisTV Revamped** Android entertainment app. Built with **React 18 + Vite + TypeScript (TSX) + Tailwind CSS + Lucide Icons**.

## Features

- **Direct APK Distribution**: Direct, one-click download for `nistv-v1.13.0.apk` with confetti feedback and fallback mirror triggers.
- **Instant Mobile QR Code**: On-screen dynamic QR code for instant phone/tablet camera scanning.
- **Cryptographic Signatures**: Integrated SHA-256 and MD5 checksums with 1-click clipboard copy for tamper verification.
- **Android TV Sideloading Guide**: Dedicated instructions and direct short links for the TV "Downloader" app.
- **Interactive App Explorer**: Tabbed showcase of Live TV & EPG, Movies & Series VOD, Resilient Downloader, ExoPlayer PiP, and Regional Language Onboarding.
- **Full SEO & Structured Data**: Built-in OpenGraph cards, Twitter preview cards, and Google Schema `SoftwareApplication` JSON-LD rich snippets.
- **Dark Obsidian Aesthetics**: Premium streaming app aesthetic with glassmorphism, responsive 44px+ touch targets, and accessible ARIA attributes.

## Quick Start

### Development
```bash
cd website
npm run dev
```

### Production Build
```bash
cd website
npm run build
```
The optimized production bundle is generated in `website/dist/` alongside the APK download binary in `website/dist/downloads/nistv-v1.13.0.apk`.

### Preview Production Build
```bash
cd website
npm run preview
```

## Deployment Options

- **Static Hosting (Cloudflare Pages, Netlify, Vercel, GitHub Pages)**: Deploy the `website/dist/` directory. All asset paths are relative (`./`) for universal compatibility.
- **Nginx / Apache**: Point root to `website/dist/` with standard static file serving and `application/vnd.android.package-archive` MIME type for `.apk`.

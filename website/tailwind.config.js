/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#f0f5ff',
          100: '#e0ecff',
          200: '#c7dcff',
          300: '#a1c4fd',
          400: '#71a3fb',
          500: '#437df4',
          600: '#2b5ee8',
          700: '#2047d4',
          800: '#1e3bad',
          900: '#1d3588',
          950: '#142153',
        },
        accent: {
          cyan: '#00f2fe',
          teal: '#4facfe',
          purple: '#7928ca',
          pink: '#ff0080',
          orange: '#ff8a00',
          emerald: '#10b981',
        },
        surface: {
          darker: '#07090e',
          dark: '#0c1017',
          card: '#131822',
          border: 'rgba(255, 255, 255, 0.08)',
          hover: '#1c2433',
        }
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'BlinkMacSystemFont', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
      animation: {
        'pulse-subtle': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'float': 'float 6s ease-in-out infinite',
        'glow': 'glow 4s ease-in-out infinite alternate',
      },
      keyframes: {
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-10px)' },
        },
        glow: {
          '0%': { opacity: '0.4', filter: 'blur(20px)' },
          '100%': { opacity: '0.8', filter: 'blur(35px)' },
        }
      }
    },
  },
  plugins: [],
}

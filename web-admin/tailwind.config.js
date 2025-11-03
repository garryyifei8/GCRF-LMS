/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./src/**/*.{vue,js,ts,jsx,tsx}",
  ],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        'ai-primary': '#667eea',
        'ai-secondary': '#764ba2',
      },
      backgroundImage: {
        'ai-gradient': 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
        'ai-gradient-hover': 'linear-gradient(135deg, #5568d3 0%, #6a4291 100%)',
      },
    },
  },
  plugins: [],
}

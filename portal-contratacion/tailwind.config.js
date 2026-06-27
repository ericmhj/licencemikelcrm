/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        primary: '#1976D2',
        success: '#4CAF50',
        warning: '#FF9800',
        danger: '#F44336',
        muted: '#9E9E9E',
      },
      screens: {
        sm: '768px',
        md: '1024px',
        lg: '1280px',
      },
    },
  },
  plugins: [],
};

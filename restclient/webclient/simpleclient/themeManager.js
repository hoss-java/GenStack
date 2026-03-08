class ThemeManager {
    constructor() {
        this.themes = ['dark', 'light', 'minimal', 'compact', 'modern'];
        this.currentTheme = this.loadTheme();
        this.init();
    }

    init() {
        this.applyTheme(this.currentTheme);
        this.attachEventListeners();
    }

    loadTheme() {
        // Check localStorage first
        const saved = localStorage.getItem('app-theme');
        if (saved && this.themes.includes(saved)) {
            return saved;
        }
        
        // Fall back to HTML data-theme attribute
        const htmlTheme = document.documentElement.getAttribute('data-theme');
        return htmlTheme && this.themes.includes(htmlTheme) ? htmlTheme : 'dark';
    }

    applyTheme(themeName) {
        if (!this.themes.includes(themeName)) {
            console.warn(`Theme "${themeName}" not found. Using default.`);
            return;
        }

        // Update HTML attribute
        document.documentElement.setAttribute('data-theme', themeName);
        
        // Update localStorage
        localStorage.setItem('app-theme', themeName);
        
        // Update active button state
        document.querySelectorAll('.theme-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.theme === themeName);
        });

        // Dispatch custom event so other scripts can react
        window.dispatchEvent(new CustomEvent('themechange', { 
            detail: { theme: themeName } 
        }));

        this.currentTheme = themeName;
    }

    attachEventListeners() {
        document.querySelectorAll('.theme-btn').forEach(btn => {
            btn.addEventListener('click', () => {
                const theme = btn.dataset.theme;
                this.applyTheme(theme);
            });
        });
    }

    getTheme() {
        return this.currentTheme;
    }
}

// Initialize theme manager when DOM is ready
if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', () => {
        window.themeManager = new ThemeManager();
    });
} else {
    window.themeManager = new ThemeManager();
}

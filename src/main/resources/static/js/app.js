// ============================================
// Furent — Client-side interactions
// ============================================

document.addEventListener('DOMContentLoaded', () => {

    // ========================================
    // Mobile Drawer Menu
    // ========================================
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const mobileCloseBtn = document.getElementById('mobileCloseBtn');
    const mobileOverlay = document.getElementById('mobileOverlay');
    const mobileDrawer = document.getElementById('mobileDrawer');

    function openDrawer() {
        if (mobileOverlay && mobileDrawer) {
            mobileOverlay.style.display = 'block';
            mobileDrawer.style.display = 'flex';
            document.body.style.overflow = 'hidden';
        }
    }

    function closeDrawer() {
        if (mobileOverlay && mobileDrawer) {
            mobileOverlay.style.display = 'none';
            mobileDrawer.style.display = 'none';
            document.body.style.overflow = '';
        }
    }

    if (mobileMenuBtn) mobileMenuBtn.addEventListener('click', openDrawer);
    if (mobileCloseBtn) mobileCloseBtn.addEventListener('click', closeDrawer);
    if (mobileOverlay) mobileOverlay.addEventListener('click', closeDrawer);

    // ========================================
    // Navbar Scroll Effect (Hide/Show + Background)
    // ========================================
    const mainNav = document.getElementById('mainNav');
    let lastScrollY = 0;
    let ticking = false;
    const scrollThreshold = 80; // Minimum scroll before hiding
    const scrollDelta = 10; // Minimum scroll delta to trigger hide/show

    if (mainNav) {
        function updateNav() {
            const scrollY = window.scrollY;

            // Background effect - Always solid as requested
            mainNav.classList.remove('nav-transparent');
            mainNav.classList.add('nav-solid');

            // Hide/Show on scroll direction
            if (scrollY > scrollThreshold) {
                if (scrollY > lastScrollY + scrollDelta) {
                    // Scrolling DOWN - hide navbar
                    mainNav.classList.add('nav-hidden');
                    mainNav.classList.remove('nav-visible');
                } else if (scrollY < lastScrollY - scrollDelta) {
                    // Scrolling UP - show navbar
                    mainNav.classList.remove('nav-hidden');
                    mainNav.classList.add('nav-visible');
                }
            } else {
                // Near the top - always show
                mainNav.classList.remove('nav-hidden');
                mainNav.classList.add('nav-visible');
            }

            lastScrollY = scrollY;
            ticking = false;
        }

        // Navbar starts solid
        mainNav.classList.remove('nav-transparent');
        mainNav.classList.add('nav-solid');

        window.addEventListener('scroll', () => {
            if (!ticking) {
                window.requestAnimationFrame(updateNav);
                ticking = true;
            }
        }, { passive: true });

        updateNav();
    }

    // ========================================
    // Active nav link
    // ========================================
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('.nav-link');
    navLinks.forEach(link => {
        const href = link.getAttribute('href');
        if (href === currentPath || (currentPath === '/' && href === '/')) {
            link.classList.add('active');
        }
    });

    // ========================================
    // Catalog filter sidebar toggle (Mobile)
    // ========================================
    const filterToggle = document.getElementById('filterToggle');
    const filterSidebar = document.getElementById('filterSidebar');

    if (filterToggle && filterSidebar) {
        filterToggle.addEventListener('click', () => {
            filterSidebar.classList.toggle('hidden');
        });
    }

    // ========================================
    // User Panel Tabs
    // ========================================
    const tabButtons = document.querySelectorAll('[data-tab]');
    const tabPanels = document.querySelectorAll('[data-tab-panel]');

    tabButtons.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.getAttribute('data-tab');

            tabButtons.forEach(b => {
                b.classList.remove('text-furent-600', 'border-furent-500');
                b.classList.add('text-surface-500', 'border-transparent');
            });
            btn.classList.add('text-furent-600', 'border-furent-500');
            btn.classList.remove('text-surface-500', 'border-transparent');

            tabPanels.forEach(panel => {
                panel.classList.add('hidden');
                if (panel.getAttribute('data-tab-panel') === tabName) {
                    panel.classList.remove('hidden');
                }
            });
        });
    });

    // ========================================
    // Settings Page Tabs
    // ========================================
    const settingsTabBtns = document.querySelectorAll('[data-settings-tab]');
    const settingsTabPanels = document.querySelectorAll('[data-settings-panel]');

    settingsTabBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            const tabName = btn.getAttribute('data-settings-tab');

            settingsTabBtns.forEach(b => {
                b.classList.remove('bg-white', 'shadow-sm', 'text-surface-900');
                b.classList.add('text-surface-500', 'hover:text-surface-700');
            });
            btn.classList.add('bg-white', 'shadow-sm', 'text-surface-900');
            btn.classList.remove('text-surface-500', 'hover:text-surface-700');

            settingsTabPanels.forEach(panel => {
                panel.classList.add('hidden');
                if (panel.getAttribute('data-settings-panel') === tabName) {
                    panel.classList.remove('hidden');
                }
            });
        });
    });

    // ========================================
    // Scroll Animations (Intersection Observer)
    // ========================================
    const animatedElements = document.querySelectorAll('.fade-in, .fade-in-up, .slide-in-left');
    if (animatedElements.length > 0) {
        const observer = new IntersectionObserver((entries) => {
            entries.forEach(entry => {
                if (entry.isIntersecting) {
                    entry.target.style.animationPlayState = 'running';
                    observer.unobserve(entry.target);
                }
            });
        }, { threshold: 0.1, rootMargin: '0px 0px -40px 0px' });

        animatedElements.forEach(el => {
            el.style.animationPlayState = 'paused';
            observer.observe(el);
        });
    }

    // ========================================
    // Date picker & total price (Product Detail)
    // ========================================
    const startDateInput = document.getElementById('startDate');
    const endDateInput = document.getElementById('endDate');
    const quantityInput = document.getElementById('quantityInput');
    const totalPriceEl = document.getElementById('totalPrice');
    const pricePerDay = parseFloat(document.getElementById('pricePerDay')?.textContent?.replace('$', '') || 0);

    function calculateTotal() {
        if (!startDateInput?.value || !endDateInput?.value) return;
        const start = new Date(startDateInput.value);
        const end = new Date(endDateInput.value);
        const diffTime = Math.abs(end - start);
        const diffDays = Math.max(1, Math.ceil(diffTime / (1000 * 60 * 60 * 24)));
        const qty = parseInt(quantityInput?.value || 1);
        const total = pricePerDay * diffDays * qty;
        if (totalPriceEl) totalPriceEl.textContent = '$' + total.toFixed(2);
    }

    if (startDateInput) startDateInput.addEventListener('change', calculateTotal);
    if (endDateInput) endDateInput.addEventListener('change', calculateTotal);

    // ========================================
    // Quantity Controls (Product Detail)
    // ========================================
    const decreaseBtn = document.getElementById('decreaseQty');
    const increaseBtn = document.getElementById('increaseQty');

    if (decreaseBtn && increaseBtn && quantityInput) {
        decreaseBtn.addEventListener('click', () => {
            const min = parseInt(quantityInput.getAttribute('min') || 1);
            let val = parseInt(quantityInput.value);
            if (val > min) {
                quantityInput.value = val - 1;
                calculateTotal();
            }
        });
        increaseBtn.addEventListener('click', () => {
            const max = parseInt(quantityInput.getAttribute('max') || 100);
            let val = parseInt(quantityInput.value);
            if (val < max) {
                quantityInput.value = val + 1;
                calculateTotal();
            }
        });
    }

    // ========================================
    // Settings Page - Language & Currency Handlers
    // ========================================
    const languageSelect = document.getElementById('languageSelect');
    const currencySelect = document.getElementById('currencySelect');

    if (languageSelect) {
        languageSelect.addEventListener('change', (e) => {
            const lang = e.target.value;
            localStorage.setItem('furent_language', lang);
            // Show a brief toast notification
            showToast('Idioma actualizado a ' + e.target.options[e.target.selectedIndex].text);
        });

        // Load saved language
        const savedLang = localStorage.getItem('furent_language');
        if (savedLang) languageSelect.value = savedLang;
    }

    if (currencySelect) {
        currencySelect.addEventListener('change', (e) => {
            const currency = e.target.value;
            localStorage.setItem('furent_currency', currency);
            showToast('Moneda actualizada a ' + e.target.options[e.target.selectedIndex].text);
        });

        // Load saved currency
        const savedCurrency = localStorage.getItem('furent_currency');
        if (savedCurrency) currencySelect.value = savedCurrency;
    }

    // ========================================
    // Global Bag Shared Logic
    // ========================================
    window.updateBagCount = function() {
        let bag = [];
        try { bag = JSON.parse(localStorage.getItem('furent_bag') || '[]'); } catch(e) {}
        const count = bag.reduce((sum, item) => sum + item.qty, 0);
        
        // Update bag counts in any page that has them
        const bagCountEls = document.querySelectorAll('#bagCount, #cotBagCount');
        bagCountEls.forEach(el => {
            el.textContent = count;
        });

        // Toggle visibility of floating bag if it exists (catalog page)
        const floatingBag = document.getElementById('floatingBag');
        if (floatingBag) {
            floatingBag.classList.toggle('hidden', bag.length === 0);
        }
    };

    // Initial check on load
    updateBagCount();

});

// ========================================
// Toast Notification
// ========================================
function showToast(message) {
    // Remove existing toasts
    const existing = document.querySelector('.furent-toast');
    if (existing) existing.remove();

    const toast = document.createElement('div');
    toast.className = 'furent-toast fixed bottom-6 right-6 z-[100] bg-surface-900 text-white px-5 py-3 rounded-xl text-sm font-medium shadow-lg transform translate-y-4 opacity-0 transition-all duration-300';
    toast.textContent = message;
    document.body.appendChild(toast);

    requestAnimationFrame(() => {
        toast.style.transform = 'translateY(0)';
        toast.style.opacity = '1';
    });

    setTimeout(() => {
        toast.style.transform = 'translateY(4px)';
        toast.style.opacity = '0';
        setTimeout(() => toast.remove(), 300);
    }, 2500);
}

// ========================================
// Button Spinner on Form Submit
// ========================================
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('form[data-spinner]').forEach(form => {
        form.addEventListener('submit', () => {
            const btn = form.querySelector('button[type="submit"]');
            if (btn && !btn.disabled) {
                btn.disabled = true;
                btn.dataset.originalText = btn.innerHTML;
                btn.innerHTML = '<svg class="animate-spin h-5 w-5 inline-block mr-2" viewBox="0 0 24 24"><circle class="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" stroke-width="4" fill="none"/><path class="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"/></svg>Procesando...';
            }
        });
    });
});

// ========================================
// Reusable Client-Side Pagination
// ========================================
class FurentPaginator {
    constructor({ containerId, itemSelector, perPage = 12, countSelector = null, emptySelector = null }) {
        this.container = document.getElementById(containerId);
        if (!this.container) return;
        this.itemSelector = itemSelector;
        this.perPage = perPage;
        this.currentPage = 1;
        this.countEl = countSelector ? document.querySelector(countSelector) : null;
        this.emptyEl = emptySelector ? document.getElementById(emptySelector) : null;
        this._createPaginationUI();
        this.update();
    }

    getVisibleItems() {
        return [...this.container.querySelectorAll(this.itemSelector)]
            .filter(el => el.style.display !== 'none' && !el.classList.contains('pagination-hidden'));
    }

    getAllItems() {
        return [...this.container.querySelectorAll(this.itemSelector)];
    }

    update() {
        const items = this.getAllItems();
        const filtered = items.filter(el => !el.dataset.searchHidden);
        const totalPages = Math.max(1, Math.ceil(filtered.length / this.perPage));
        if (this.currentPage > totalPages) this.currentPage = totalPages;
        const start = (this.currentPage - 1) * this.perPage;
        const end = start + this.perPage;

        filtered.forEach((item, i) => {
            item.style.display = (i >= start && i < end) ? '' : 'none';
        });
        items.filter(el => el.dataset.searchHidden).forEach(el => el.style.display = 'none');

        if (this.countEl) this.countEl.textContent = filtered.length;
        if (this.emptyEl) {
            this.emptyEl.classList.toggle('hidden', filtered.length > 0);
        }
        this._renderControls(totalPages, filtered.length);
    }

    filterBySearch(term) {
        this.currentPage = 1;
        const items = this.getAllItems();
        items.forEach(item => {
            const searchData = (item.dataset.search || item.textContent).toLowerCase();
            item.dataset.searchHidden = searchData.includes(term.toLowerCase()) ? '' : 'true';
            if (!searchData.includes(term.toLowerCase())) {
                item.dataset.searchHidden = 'true';
            } else {
                delete item.dataset.searchHidden;
            }
        });
        this.update();
    }

    goTo(page) {
        this.currentPage = page;
        this.update();
    }

    _createPaginationUI() {
        this.paginationEl = document.createElement('div');
        this.paginationEl.className = 'flex items-center justify-center gap-2 mt-8 mb-4';
        this.container.parentNode.insertBefore(this.paginationEl, this.container.nextSibling);
    }

    _renderControls(totalPages, totalItems) {
        if (totalPages <= 1) { this.paginationEl.innerHTML = ''; return; }
        const btnClass = 'px-3 py-2 text-sm font-semibold rounded-lg transition-all ';
        const activeClass = btnClass + 'bg-surface-900 text-white shadow-sm';
        const normalClass = btnClass + 'bg-white text-surface-600 border border-surface-200 hover:bg-surface-50';
        const disabledClass = btnClass + 'bg-surface-100 text-surface-300 cursor-not-allowed';

        let html = '';
        html += `<button class="${this.currentPage === 1 ? disabledClass : normalClass}" data-page="${this.currentPage - 1}" ${this.currentPage === 1 ? 'disabled' : ''}>‹</button>`;

        const range = this._pageRange(this.currentPage, totalPages);
        for (const p of range) {
            if (p === '...') {
                html += `<span class="px-2 text-surface-400">…</span>`;
            } else {
                html += `<button class="${p === this.currentPage ? activeClass : normalClass}" data-page="${p}">${p}</button>`;
            }
        }

        html += `<button class="${this.currentPage === totalPages ? disabledClass : normalClass}" data-page="${this.currentPage + 1}" ${this.currentPage === totalPages ? 'disabled' : ''}>›</button>`;
        html += `<span class="ml-3 text-xs text-surface-400">${totalItems} registros</span>`;

        this.paginationEl.innerHTML = html;
        this.paginationEl.querySelectorAll('button[data-page]').forEach(btn => {
            btn.addEventListener('click', () => {
                const p = parseInt(btn.dataset.page);
                if (p >= 1 && p <= totalPages) this.goTo(p);
            });
        });
    }

    _pageRange(current, total) {
        if (total <= 7) return Array.from({length: total}, (_, i) => i + 1);
        const pages = [];
        pages.push(1);
        if (current > 3) pages.push('...');
        for (let i = Math.max(2, current - 1); i <= Math.min(total - 1, current + 1); i++) pages.push(i);
        if (current < total - 2) pages.push('...');
        pages.push(total);
        return pages;
    }
}

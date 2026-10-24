// portfolio.js - Логика страницы портфолио

document.addEventListener('DOMContentLoaded', () => {

    // 1. Мобильное меню
    const mobileMenuBtn = document.getElementById('mobileMenuBtn');
    const navbarNav = document.getElementById('navbarNav');

    if (mobileMenuBtn && navbarNav) {
        mobileMenuBtn.addEventListener('click', () => {
            navbarNav.classList.toggle('open');
        });

        navbarNav.querySelectorAll('a').forEach(link => {
            link.addEventListener('click', () => {
                navbarNav.classList.remove('open');
            });
        });
    }

    // 2. Элементы фильтрации
    const filterBtns = document.querySelectorAll('.filter-btn');
    const subfilterBtns = document.querySelectorAll('.subfilter-btn');
    const subcategoryBlocks = document.querySelectorAll('.subcategory-block');
    const noResults = document.getElementById('noResults');

    const tagFilterAnime = document.getElementById('tagFilterAnime');
    const tagFilterLocation = document.getElementById('tagFilterLocation');
    const tagFilterStyle = document.getElementById('tagFilterStyle');
    const resetTagsBtn = document.getElementById('resetTagsBtn');

    let currentCategory = 'all';
    let currentSubcategory = 'all';
    let currentAnime = 'all';
    let currentLocation = 'all';
    let currentStyle = 'all';

    // Заполнение выпадающих списков тегов уникальными значениями из страницы
    function initTagFilters() {
        const animeSet = new Set();
        const locationSet = new Set();
        const styleSet = new Set();

        subcategoryBlocks.forEach(block => {
            const anime = block.getAttribute('data-anime');
            const location = block.getAttribute('data-location');
            const style = block.getAttribute('data-style');

            if (anime) animeSet.add(anime.trim());
            if (location) locationSet.add(location.trim());
            if (style) styleSet.add(style.trim());
        });

        populateSelect(tagFilterAnime, animeSet);
        populateSelect(tagFilterLocation, locationSet);
        populateSelect(tagFilterStyle, styleSet);
    }

    function populateSelect(selectElement, valueSet) {
        if (!selectElement) return;
        const sorted = Array.from(valueSet).sort((a, b) => a.localeCompare(b, 'ru'));
        sorted.forEach(val => {
            const opt = document.createElement('option');
            opt.value = val;
            opt.textContent = val;
            selectElement.appendChild(opt);
        });
    }

    // Применение комплексной фильтрации (категории + подкатегории + теги)
    function applyFilters() {
        let visibleCount = 0;

        subcategoryBlocks.forEach(block => {
            const blockCat = block.getAttribute('data-category');
            const blockSub = block.getAttribute('data-subcategory');
            const blockAnime = block.getAttribute('data-anime') || '';
            const blockLocation = block.getAttribute('data-location') || '';
            const blockStyle = block.getAttribute('data-style') || '';

            const matchesCat = (currentCategory === 'all' || blockCat === currentCategory);
            const matchesSub = (currentSubcategory === 'all' || blockSub === currentSubcategory);
            const matchesAnime = (currentAnime === 'all' || blockAnime === currentAnime);
            const matchesLocation = (currentLocation === 'all' || blockLocation === currentLocation);
            const matchesStyle = (currentStyle === 'all' || blockStyle === currentStyle);

            const isVisible = matchesCat && matchesSub && matchesAnime && matchesLocation && matchesStyle;
            block.classList.toggle('hidden-group', !isVisible);

            if (isVisible) visibleCount++;
        });

        // Управление кнопкой сброса тегов
        const hasActiveTags = currentAnime !== 'all' || currentLocation !== 'all' || currentStyle !== 'all';
        if (resetTagsBtn) {
            resetTagsBtn.classList.toggle('hidden-reset', !hasActiveTags);
        }

        if (noResults) {
            noResults.classList.toggle('visible', visibleCount === 0);
        }
    }

    // Инициализируем опции в селектах
    initTagFilters();

    // Слушатели событий селектов тегов
    if (tagFilterAnime) {
        tagFilterAnime.addEventListener('change', (e) => {
            currentAnime = e.target.value;
            applyFilters();
        });
    }

    if (tagFilterLocation) {
        tagFilterLocation.addEventListener('change', (e) => {
            currentLocation = e.target.value;
            applyFilters();
        });
    }

    if (tagFilterStyle) {
        tagFilterStyle.addEventListener('change', (e) => {
            currentStyle = e.target.value;
            applyFilters();
        });
    }

    // Сброс тегов
    if (resetTagsBtn) {
        resetTagsBtn.addEventListener('click', () => {
            currentAnime = 'all';
            currentLocation = 'all';
            currentStyle = 'all';
            if (tagFilterAnime) tagFilterAnime.value = 'all';
            if (tagFilterLocation) tagFilterLocation.value = 'all';
            if (tagFilterStyle) tagFilterStyle.value = 'all';
            applyFilters();
        });
    }

    // Клик по кликабельным бейджикам тегов на карточках
    document.querySelectorAll('.tag-chip').forEach(chip => {
        chip.addEventListener('click', (e) => {
            e.stopPropagation();
            const type = chip.getAttribute('data-tag-type');
            const val = chip.getAttribute('data-tag-val');

            if (type === 'anime') {
                currentAnime = val;
                if (tagFilterAnime) tagFilterAnime.value = val;
            } else if (type === 'location') {
                currentLocation = val;
                if (tagFilterLocation) tagFilterLocation.value = val;
            } else if (type === 'style') {
                currentStyle = val;
                if (tagFilterStyle) tagFilterStyle.value = val;
            }

            applyFilters();

            // Плавный скролл к панели фильтрации
            const header = document.querySelector('.portfolio-header');
            if (header) {
                header.scrollIntoView({ behavior: 'smooth' });
            }
        });
    });

    // Клик по основной категории
    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            currentCategory = btn.getAttribute('data-filter');
            currentSubcategory = 'all';

            // Обновляем видимость подкатегорий
            subfilterBtns.forEach(subBtn => {
                const parentCat = subBtn.getAttribute('data-parent');
                subBtn.classList.remove('active');

                if (subBtn.getAttribute('data-subfilter') === 'all') {
                    subBtn.classList.add('active');
                    subBtn.classList.remove('hidden-subfilter');
                } else if (currentCategory === 'all' || parentCat === currentCategory) {
                    subBtn.classList.remove('hidden-subfilter');
                } else {
                    subBtn.classList.add('hidden-subfilter');
                }
            });

            applyFilters();
        });
    });

    // Клик по подкатегории
    subfilterBtns.forEach(subBtn => {
        subBtn.addEventListener('click', () => {
            subfilterBtns.forEach(b => b.classList.remove('active'));
            subBtn.classList.add('active');

            currentSubcategory = subBtn.getAttribute('data-subfilter');
            applyFilters();
        });
    });

    // 3. Лайтбокс (Увеличение фото по клику на любую карточку)
    const lightbox = document.getElementById('portfolioLightbox');
    const lightboxImg = document.getElementById('lightboxImg');
    const lightboxCaption = document.getElementById('lightboxCaption');
    const lightboxClose = document.getElementById('lightboxClose');

    document.querySelectorAll('.gallery-item').forEach(item => {
        item.addEventListener('click', () => {
            const img = item.querySelector('img');
            if (img) {
                lightboxImg.src = img.src;

                // Получаем инфу о блоке (название и теги) для подписи
                const parentBlock = item.closest('.subcategory-block');
                if (parentBlock) {
                    const title = parentBlock.querySelector('.subcategory-title')?.textContent || '';
                    const anime = parentBlock.getAttribute('data-anime') || '';
                    const location = parentBlock.getAttribute('data-location') || '';
                    const style = parentBlock.getAttribute('data-style') || '';

                    const tagsArr = [];
                    if (anime) tagsArr.push('🎌 ' + anime);
                    if (location) tagsArr.push('📍 ' + location);
                    if (style) tagsArr.push('🎨 ' + style);

                    let captionText = title;
                    if (tagsArr.length > 0) {
                        captionText += ' (' + tagsArr.join(' • ') + ')';
                    }
                    lightboxCaption.textContent = captionText;
                } else {
                    lightboxCaption.textContent = img.alt || '';
                }

                lightbox.classList.add('active');
            }
        });
    });

    // Закрытие по крестику
    if (lightboxClose) {
        lightboxClose.addEventListener('click', () => {
            lightbox.classList.remove('active');
        });
    }

    // Закрытие при клике по фону
    if (lightbox) {
        lightbox.addEventListener('click', (e) => {
            if (e.target === lightbox) {
                lightbox.classList.remove('active');
            }
        });
    }

    // Закрытие по клавише Esc
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && lightbox && lightbox.classList.contains('active')) {
            lightbox.classList.remove('active');
        }
    });
});

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

        const filterCat = (currentCategory || 'all').toLowerCase().trim();
        const filterSub = (currentSubcategory || 'all').toLowerCase().trim();
        const filterAnime = (currentAnime || 'all').toLowerCase().trim();
        const filterLocation = (currentLocation || 'all').toLowerCase().trim();
        const filterStyle = (currentStyle || 'all').toLowerCase().trim();

        subcategoryBlocks.forEach(block => {
            const blockCat = (block.getAttribute('data-category') || '').toLowerCase().trim();
            const blockSub = (block.getAttribute('data-subcategory') || '').toLowerCase().trim();
            const blockAnime = (block.getAttribute('data-anime') || '').toLowerCase().trim();
            const blockLocation = (block.getAttribute('data-location') || '').toLowerCase().trim();
            const blockStyle = (block.getAttribute('data-style') || '').toLowerCase().trim();

            // Если у карточки несколько категорий, фильтрация использует проверку вхождения (.includes())
            const matchesCat = (filterCat === 'all' || blockCat.includes(filterCat));
            const matchesSub = (filterSub === 'all' || blockSub.includes(filterSub));
            const matchesAnime = (filterAnime === 'all' || blockAnime.includes(filterAnime));
            const matchesLocation = (filterLocation === 'all' || blockLocation.includes(filterLocation));
            const matchesStyle = (filterStyle === 'all' || blockStyle.includes(filterStyle));

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

            // Приводим data-filter к нижнему регистру
            currentCategory = (btn.getAttribute('data-filter') || 'all').toLowerCase().trim();
            currentSubcategory = 'all';

            // Обновляем видимость подкатегорий
            subfilterBtns.forEach(subBtn => {
                const parentCat = (subBtn.getAttribute('data-parent') || '').toLowerCase().trim();
                subBtn.classList.remove('active');

                const subVal = (subBtn.getAttribute('data-subfilter') || '').toLowerCase().trim();
                if (subVal === 'all') {
                    subBtn.classList.add('active');
                    subBtn.classList.remove('hidden-subfilter');
                } else if (currentCategory === 'all' || parentCat.includes(currentCategory) || currentCategory.includes(parentCat)) {
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

            // Приводим data-subfilter к нижнему регистру
            currentSubcategory = (subBtn.getAttribute('data-subfilter') || 'all').toLowerCase().trim();
            applyFilters();
        });
    });

    // 3. Лайтбокс и листание фото в рамках одной подкатегории (фотосета)
    const lightbox = document.getElementById('portfolioLightbox');
    const lightboxImg = document.getElementById('lightboxImg');
    const lightboxCaption = document.getElementById('lightboxCaption');
    const lightboxClose = document.getElementById('lightboxClose');
    const lightboxPrev = document.getElementById('lightboxPrev');
    const lightboxNext = document.getElementById('lightboxNext');

    let currentSubcategoryPhotos = [];
    let currentPhotoIndex = -1;

    function showPhotoAtIndex(index) {
        if (index < 0 || index >= currentSubcategoryPhotos.length) return;
        currentPhotoIndex = index;
        const photoItem = currentSubcategoryPhotos[currentPhotoIndex];
        const img = photoItem.querySelector('img');
        if (!img) return;

        // Безопасная валидация протокола источника изображения (предотвращение javascript: URI)
        const imgSrc = img.getAttribute('src') || img.src || '';
        if (imgSrc.startsWith('javascript:') || imgSrc.startsWith('vbscript:')) {
            lightboxImg.src = 'img/placeholder.webp';
        } else {
            lightboxImg.src = imgSrc;
        }
        lightboxImg.alt = img.alt || '';

        // Текст оверлея или подписи для лайтбокса
        const overlayText = photoItem.querySelector('.portfolio-overlay')?.textContent?.trim() || '';

        const parentBlock = photoItem.closest('.subcategory-block');
        if (parentBlock) {
            const title = parentBlock.querySelector('.subcategory-title')?.textContent || '';
            const anime = parentBlock.getAttribute('data-anime') || '';
            const location = parentBlock.getAttribute('data-location') || '';
            const style = parentBlock.getAttribute('data-style') || '';

            const tagsArr = [];
            if (anime) tagsArr.push('🎌 ' + anime);
            if (location) tagsArr.push('📍 ' + location);
            if (style) tagsArr.push('🎨 ' + style);

            let captionText = overlayText ? overlayText : title;
            if (tagsArr.length > 0) {
                captionText += ' (' + tagsArr.join(' • ') + ')';
            }
            captionText += ` [ ${currentPhotoIndex + 1} / ${currentSubcategoryPhotos.length} ]`;
            lightboxCaption.textContent = captionText;
        } else {
            lightboxCaption.textContent = (overlayText || img.alt || '') + ` [ ${currentPhotoIndex + 1} / ${currentSubcategoryPhotos.length} ]`;
        }

        // Обновляем видимость стрелок: если фотки в подкатегории кончаются, стрелка пропадает!
        if (lightboxPrev) {
            lightboxPrev.classList.toggle('hidden-nav', currentPhotoIndex === 0);
        }
        if (lightboxNext) {
            lightboxNext.classList.toggle('hidden-nav', currentPhotoIndex >= currentSubcategoryPhotos.length - 1);
        }
    }

    // Клик по карточке фотографии в галерее
    document.querySelectorAll('.gallery-item').forEach(item => {
        item.addEventListener('click', () => {
            const parentBlock = item.closest('.subcategory-block');
            if (!parentBlock) return;

            // Собираем фотографии ИСКЛЮЧИТЕЛЬНО из ЭТОЙ подкатегории (блока сета / косплеера)
            currentSubcategoryPhotos = Array.from(parentBlock.querySelectorAll('.gallery-item'));

            const initialIndex = currentSubcategoryPhotos.indexOf(item);
            if (initialIndex !== -1) {
                showPhotoAtIndex(initialIndex);
                lightbox.classList.add('active');
            }
        });
    });

    // Клик по стрелке «Назад»
    if (lightboxPrev) {
        lightboxPrev.addEventListener('click', (e) => {
            e.stopPropagation();
            if (currentPhotoIndex > 0) {
                showPhotoAtIndex(currentPhotoIndex - 1);
            }
        });
    }

    // Клик по стрелке «Вперед»
    if (lightboxNext) {
        lightboxNext.addEventListener('click', (e) => {
            e.stopPropagation();
            if (currentPhotoIndex < currentSubcategoryPhotos.length - 1) {
                showPhotoAtIndex(currentPhotoIndex + 1);
            }
        });
    }

    // Закрытие по крестику
    if (lightboxClose) {
        lightboxClose.addEventListener('click', (e) => {
            e.stopPropagation();
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

    // Управление с клавиатуры: Стрелки влево/вправо и Esc
    document.addEventListener('keydown', (e) => {
        if (!lightbox || !lightbox.classList.contains('active')) return;

        if (e.key === 'ArrowLeft' && currentPhotoIndex > 0) {
            showPhotoAtIndex(currentPhotoIndex - 1);
        } else if (e.key === 'ArrowRight' && currentPhotoIndex < currentSubcategoryPhotos.length - 1) {
            showPhotoAtIndex(currentPhotoIndex + 1);
        } else if (e.key === 'Escape') {
            lightbox.classList.remove('active');
        }
    });

    // Поддержка сенсорных свайпов для смартфонов
    let touchStartX = 0;
    let touchStartY = 0;

    if (lightbox) {
        lightbox.addEventListener('touchstart', (e) => {
            touchStartX = e.changedTouches[0].clientX;
            touchStartY = e.changedTouches[0].clientY;
        }, { passive: true });

        lightbox.addEventListener('touchend', (e) => {
            const touchEndX = e.changedTouches[0].clientX;
            const touchEndY = e.changedTouches[0].clientY;
            const diffX = touchEndX - touchStartX;
            const diffY = touchEndY - touchStartY;

            // Горизонтальный свайп с порогом более 40px
            if (Math.abs(diffX) > 40 && Math.abs(diffX) > Math.abs(diffY)) {
                if (diffX > 0 && currentPhotoIndex > 0) {
                    // Свайп вправо -> предыдущее фото
                    showPhotoAtIndex(currentPhotoIndex - 1);
                } else if (diffX < 0 && currentPhotoIndex < currentSubcategoryPhotos.length - 1) {
                    // Свайп влево -> следующее фото
                    showPhotoAtIndex(currentPhotoIndex + 1);
                }
            }
        }, { passive: true });
    }
});

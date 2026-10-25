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

    // 2. Элементы фильтрации (Категории + теги)
    const filterBtns = document.querySelectorAll('.filter-btn');
    const subcategoryBlocks = document.querySelectorAll('.subcategory-block');
    const noResults = document.getElementById('noResults');

    let currentCategory = 'all';
    let currentAnime = 'all';
    let currentLocation = 'all';
    let currentStyle = 'all';

    // Применение фильтрации (по 5 основным категориям и тегам)
    function applyFilters() {
        let visibleCount = 0;

        const filterCat = (currentCategory || 'all').toLowerCase().trim();
        const filterAnime = (currentAnime || 'all').toLowerCase().trim();
        const filterLocation = (currentLocation || 'all').toLowerCase().trim();
        const filterStyle = (currentStyle || 'all').toLowerCase().trim();

        subcategoryBlocks.forEach(block => {
            const blockCat = (block.getAttribute('data-category') || '').toLowerCase().trim();
            const blockAnime = (block.getAttribute('data-anime') || '').toLowerCase().trim();
            const blockLocation = (block.getAttribute('data-location') || '').toLowerCase().trim();
            const blockStyle = (block.getAttribute('data-style') || '').toLowerCase().trim();

            // Проверка категории
            const matchesCat = (filterCat === 'all' || blockCat.includes(filterCat));
            const matchesAnime = (filterAnime === 'all' || blockAnime.includes(filterAnime));
            const matchesLocation = (filterLocation === 'all' || blockLocation.includes(filterLocation));
            const matchesStyle = (filterStyle === 'all' || blockStyle.includes(filterStyle));

            const isVisible = matchesCat && matchesAnime && matchesLocation && matchesStyle;
            block.classList.toggle('hidden-group', !isVisible);

            if (isVisible) visibleCount++;
        });

        if (noResults) {
            noResults.classList.toggle('visible', visibleCount === 0);
        }
    }

    // Клик по категории: [ Все ], [ Студия & Арт ], [ Улица & Локации ], [ Лайфстайл ], [ Фестивали ]
    filterBtns.forEach(btn => {
        btn.addEventListener('click', () => {
            filterBtns.forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            currentCategory = (btn.getAttribute('data-filter') || 'all').toLowerCase().trim();
            // Сбрасываем выбранные теги при переключении категории
            currentAnime = 'all';
            currentLocation = 'all';
            currentStyle = 'all';
            document.querySelectorAll('.tag-chip').forEach(c => c.classList.remove('active-tag'));

            applyFilters();
        });
    });

    // Клик по тегам на самих карточках
    document.querySelectorAll('.tag-chip').forEach(chip => {
        chip.addEventListener('click', (e) => {
            e.stopPropagation();
            const type = chip.getAttribute('data-tag-type');
            const val = (chip.getAttribute('data-tag-val') || '').toLowerCase().trim();

            if (type === 'anime') {
                currentAnime = (currentAnime === val) ? 'all' : val;
            } else if (type === 'location') {
                currentLocation = (currentLocation === val) ? 'all' : val;
            } else if (type === 'style') {
                currentStyle = (currentStyle === val) ? 'all' : val;
            }

            chip.classList.toggle('active-tag', (currentAnime === val || currentLocation === val || currentStyle === val) && val !== 'all');
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

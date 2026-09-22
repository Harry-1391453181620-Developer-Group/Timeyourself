function setupHoverGroup(sectionId, trapezoidId, chooseBarId) {
    const section = document.getElementById(sectionId);
    const trapezoid = document.getElementById(trapezoidId);
    const chooseBar = document.getElementById(chooseBarId);

    const group = [section, trapezoid, chooseBar].filter(Boolean);

    function show() {
        if (trapezoid) trapezoid.style.display = 'block';
        if (chooseBar) chooseBar.style.display = 'flex';
    }

    function hide(e) {
        const nextTarget = e.relatedTarget;

        const isStayingInGroup = group.some(
            el => el === nextTarget || el.contains(nextTarget)
        );

        if (!isStayingInGroup) {
            if (trapezoid) trapezoid.style.display = 'none';
            if (chooseBar) chooseBar.style.display = 'none';
        }
    }

    group.forEach(element => {
        element.addEventListener('mouseenter', show);
        element.addEventListener('mouseleave', hide);
    });
}

setupHoverGroup('categories', 'of-categories', 'for-categories');
setupHoverGroup('plans', 'of-plans', 'for-plans');

(function () {
    'use strict';
    const LIST_VIEW = 'list';
    const LINE_VIEW = 'line';

    const DEFAULT_VIEW = LIST_VIEW;

    const ENTRY_SELECTOR = '.main-display-bar-task-entry, .main-display-bar-event-entry';

    const mainDisplayArea = document.getElementById('main-display-area');
    if (!mainDisplayArea) return;

    const listButton = document.querySelector(
        '.main-display-bar-display-mode-button-area .view-as-list-button'
    );
    const lineButton = document.querySelector(
        '.main-display-bar-display-mode-button-area .view-as-line-button'
    );

    let currentView = null;
    let savedScrollTop = 0;

    function getEntries() {
        const all = mainDisplayArea.querySelectorAll(ENTRY_SELECTOR);
        const result = [];
        for (let i = 0; i < all.length; i++) {
            if (all[i].parentElement === mainDisplayArea) result.push(all[i]);
        }
        return result;
    }

    function setActive(activeButton, inactiveButton) {
        if (activeButton) activeButton.style.backgroundColor = '#00b4d8';
        if (inactiveButton) inactiveButton.style.backgroundColor = '';
    }

    function showListView() {
        getEntries().forEach(function (entry) {
            entry.style.display = 'flex';
        });

        mainDisplayArea.style.overflowY = 'scroll';
        mainDisplayArea.style.overflowX = 'hidden';
        mainDisplayArea.style.scrollbarWidth = '';

        mainDisplayArea.scrollTop = savedScrollTop;
        setActive(listButton, lineButton);
    }

    function showLineView() {
        savedScrollTop = mainDisplayArea.scrollTop || 0;

        getEntries().forEach(function (entry) {
            entry.style.display = 'none';
        });

        mainDisplayArea.style.overflowY = 'hidden';
        mainDisplayArea.style.overflowX = 'hidden';
        mainDisplayArea.style.scrollbarWidth = 'none';
        mainDisplayArea.scrollTop = 0;
        setActive(lineButton, listButton);
    }

    function setView(mode) {
        if (mode === currentView) return;
        currentView = mode;

        if (mode === LINE_VIEW) {
            showLineView();
        } else {
            showListView();
        }
    }

    if (listButton) {
        listButton.style.cursor = 'pointer';
        listButton.setAttribute('role', 'button');
        listButton.addEventListener('click', function () {
            setView(LIST_VIEW);
        });
    }

    if (lineButton) {
        lineButton.style.cursor = 'pointer';
        lineButton.setAttribute('role', 'button');
        lineButton.addEventListener('click', function () {
            setView(LINE_VIEW);
        });
    }
    function init() {
        currentView = (DEFAULT_VIEW === LINE_VIEW) ? LIST_VIEW : LINE_VIEW;
        setView(DEFAULT_VIEW);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();
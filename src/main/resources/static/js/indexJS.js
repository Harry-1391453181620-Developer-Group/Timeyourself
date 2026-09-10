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
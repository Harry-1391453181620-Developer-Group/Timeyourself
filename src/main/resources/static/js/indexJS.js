const myDaySection = document.getElementById('day');
const categoriesSection = document.getElementById('categories');

function openDay() {
    const target = document.getElementById('main-display-area');
    target.style.backgroundColor = '#ffffff';
}

myDaySection.addEventListener('click', openDay);

// Trigger action on keyboard "Enter" key press (for accessibility)
myDaySection.addEventListener('keydown', function(event) {
    if (event.key === 'Enter') {
        openDay();
    }
});
const categoriesSection = document.getElementById('categories');
const trapezoidOfCategories = document.getElementById('of-categories');
const chooseBarForCategories = document.getElementById('for-categories');

function viewCategories() {
    if (trapezoidOfCategories) {
        trapezoidOfCategories.style.display = 'block';
    }
    if (chooseBarForCategories) {
        chooseBarForCategories.style.display = 'flex';
    }
}
function hideCategories() {
    if (trapezoidOfCategories) {
        trapezoidOfCategories.style.display = 'none';
    }
    if (chooseBarForCategories) {
        chooseBarForCategories.style.display = 'none';
    }
}
if (categoriesSection) {
    categoriesSection.addEventListener('mouseenter', viewCategories);
    categoriesSection.addEventListener('mouseleave', hideCategories);
}

const plansSection = document.getElementById('plans');
const trapezoidOfPlans = document.getElementById('of-plans');
const chooseBarForPlans = document.getElementById('for-plans');

function viewPlans() {
    if (trapezoidOfPlans) {
        trapezoidOfPlans.style.display = 'block';
    }
    if (chooseBarForPlans) {
        chooseBarForPlans.style.display = 'flex';
    }
}
function hidePlans() {
    if (trapezoidOfPlans) {
        trapezoidOfPlans.style.display = 'none';
    }
    if (chooseBarForPlans) {
        chooseBarForPlans.style.display = 'none';
    }
}
if (plansSection) {
    plansSection.addEventListener('mouseenter', viewPlans);
    plansSection.addEventListener('mouseleave', hidePlans);
}

const eventsSection = document.getElementById('events');
const trapezoidOfEvents = document.getElementById('of-events');
const chooseBarForEvents = document.getElementById('for-events');

function viewEvents() {
    if (trapezoidOfEvents) {
        trapezoidOfEvents.style.display = 'block';
    }
    if (chooseBarForEvents) {
        chooseBarForEvents.style.display = 'flex';
    }
}
function hideEvents() {
    if (trapezoidOfEvents) {
        trapezoidOfEvents.style.display = 'none';
    }
    if (chooseBarForEvents) {
        chooseBarForEvents.style.display = 'none';
    }
}
if (eventsSection) {
    eventsSection.addEventListener('mouseenter', viewEvents);
    eventsSection.addEventListener('mouseleave', hideEvents);
}
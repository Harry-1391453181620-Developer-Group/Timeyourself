const categoriesSection = document.getElementById('categories');
const trapezoidOfCategories = document.getElementById('of-categories');

function viewCategories() {
    if (trapezoidOfCategories) {
        trapezoidOfCategories.style.display = 'block';
    }
}
function hideCategories() {
    if (trapezoidOfCategories) {
        trapezoidOfCategories.style.display = 'none';
    }
}
if (categoriesSection) {
    categoriesSection.addEventListener('mouseenter', viewCategories);
    categoriesSection.addEventListener('mouseleave', hideCategories);
}

const plansSection = document.getElementById('plans');
const trapezoidOfPlans = document.getElementById('of-plans');

function viewPlans() {
    if (trapezoidOfPlans) {
        trapezoidOfPlans.style.display = 'block';
    }
}
function hidePlans() {
    if (trapezoidOfPlans) {
        trapezoidOfPlans.style.display = 'none';
    }
}
if (plansSection) {
    plansSection.addEventListener('mouseenter', viewPlans);
    plansSection.addEventListener('mouseleave', hidePlans);
}

const eventsSection = document.getElementById('events');
const trapezoidOfEvents = document.getElementById('of-events');

function viewEvents() {
    if (trapezoidOfEvents) {
        trapezoidOfEvents.style.display = 'block';
    }
}
function hideEvents() {
    if (trapezoidOfEvents) {
        trapezoidOfEvents.style.display = 'none';
    }
}
if (eventsSection) {
    eventsSection.addEventListener('mouseenter', viewEvents);
    eventsSection.addEventListener('mouseleave', hideEvents);
}
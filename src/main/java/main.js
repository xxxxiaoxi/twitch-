var gDiv = document.getElementById('grandparent');
var pDiv = document.getElementById('parent');
var cDiv = document.getElementById('child');

gDiv.addEventListener('click', function () {
    console.log("grandparent bubbling! ");
}, false);

pDiv.addEventListener('click', function () {
    console.log("parent bubbling! ");
}, false);

cDiv.addEventListener('click', function () {
    console.log("child bubbling! ");
}, false);

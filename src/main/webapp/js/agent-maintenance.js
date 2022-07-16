var startTimePicker;
var endTimePicker;

function openForm() {
    document.getElementById("maintenance-add-form").style.display = "block";
    document.addEventListener("keydown", cancelAdd);
}

function closeForm() {
    document.getElementById("maintenance-add-form").style.display = "none";
    document.removeEventListener("keydown", cancelAdd);
}

function cancelAdd(event) {
    if (event.key === 'Escape') {
        closeForm();
    }
}

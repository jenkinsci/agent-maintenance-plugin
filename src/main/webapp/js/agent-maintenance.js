var startTimePicker;
var endTimePicker;

function openForm() {
    document.getElementById("maintenance-add-form").style.display = "block";
    document.getElementById("maintenance-table").style.display = "none";
    document.addEventListener("keydown", cancelAdd);
}

function closeForm() {
    document.getElementById("maintenance-add-form").style.display = "none";
    document.getElementById("maintenance-table").style.display = "block";
    document.removeEventListener("keydown", cancelAdd);
}

function cancelAdd(event) {
    if (event.key === 'Escape') {
        closeForm();
    }
}

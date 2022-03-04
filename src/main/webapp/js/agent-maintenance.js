var startTimePicker;
var endTimePicker;

function openForm() {
    if (!startTimePicker) {
        startTimeElement = document.getElementById("startTime");
        startTimePicker = tail.DateTime(startTimeElement,{ animate: true, timeFormat: "HH:ii", weekStart: 1, timeSeconds: false, timeStepMinutes: 1 });
    }

    if (!endTimePicker) {
        endTimeElement = document.getElementById("endTime");
        endTimePicker = tail.DateTime(endTimeElement,{ animate: true, timeFormat: "HH:ii", weekStart: 1, timeSeconds: false, timeStepMinutes: 1 });
    }
    document.getElementById("maintenance-add-form").style.display = "block";
    document.getElementById("maintenance-table").style.display = "none";
    document.addEventListener("keydown", cancelAdd);
}

function closeForm() {
    document.getElementById("maintenance-add-form").style.display = "none";
    document.getElementById("maintenance-table").style.display = "block";
    document.removeEventListener("keydown", cancelAdd);
    startTimePicker.close();
    endTimePicker.close();
}

function cancelAdd(event) {
    if (event.key === 'Escape') {
        closeForm();
    }
}

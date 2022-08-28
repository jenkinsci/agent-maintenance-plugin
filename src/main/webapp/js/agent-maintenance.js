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

var selectMaintenanceWindows = function(toggle, className) {
    let table = document.getElementById("maintenance-table")
    let inputs = table.getElementsByTagName("input");
    for(var i=0; i<inputs.length; i++) {
        if(inputs[i].type === "checkbox" && !inputs[i].disabled) {
            let tr = findAncestor(inputs[i], "TR")
            if (className == null || tr != null && tr.classList.contains(className)) {
                inputs[i].checked = toggle;
            }
        }
    }
};

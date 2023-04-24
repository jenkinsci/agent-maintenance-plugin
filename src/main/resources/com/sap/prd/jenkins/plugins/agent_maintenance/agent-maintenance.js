var startTimePicker;
var endTimePicker;

function openForm() {
    document.getElementById("maintenance-add-form").style.display = "flex";
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
    let table = document.getElementById("maintenance-table");
    let inputs = table.querySelectorAll('tr' + className + ' input.am__checkbox');
    for (let input of inputs) {
      input.checked = toggle;
    }
};

Behaviour.specify(".am__action-delete", 'agent-maintenance', 0, function(e) {
  e.onclick = function () {
    let message = this.getAttribute("data-message");
    let messageSuccess = this.getAttribute("data-message-success");
    let id = this.getAttribute("data-id")
    if (confirm(message)) {
      fetch("deleteMaintenance?id=" + id, {
          method: "POST",
          headers: crumb.wrap({}),
        }
      ).then((rsp) => {
        if (rsp.ok) {
          let row = findAncestor(this, "TR");
          let tbody = row.parentNode;
          let parent = row.parentNode;
          parent.removeChild(row);
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
          if (tbody.children.length == 0) {
            document.getElementById("edit-button").style.display = "none";
            document.getElementById("delete-selected-button").style.display = "none";
          }
        }
      });
    }
  }
});

Behaviour.specify(".am__link-delete", 'agent-maintenance', 0, function(e) {
  e.onclick = function () {
    let message = this.getAttribute("data-message");
    let messageSuccess = this.getAttribute("data-message-success");
    let id = this.getAttribute("data-id");
    let url = this.getAttribute("data-url");
    if (confirm(message)) {
      fetch(url+"maintenanceWindows/deleteMaintenance?id=" + id, {
          method: "POST",
          headers: crumb.wrap({}),
        }
      ).then((rsp) => {
        if (rsp.ok) {
          let row = findAncestor(this, "TR");
          let tbody = row.parentNode;
          let parent = row.parentNode;
          parent.removeChild(row);
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
          if (tbody.children.length == 0) {
            document.getElementById("delete-selected-button").style.display = "none";
          }
        }
      });
    }
  }
});

Behaviour.specify(".am__disable", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    let message = this.getAttribute("data-message");
    if (confirm(message)) {
      fetch("disable",  {
          method: "POST",
          headers: crumb.wrap({}),
        }
      );
      location.reload();
    };
  }
});

Behaviour.specify(".am__enable", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    let message = this.getAttribute("data-message");
    if (confirm(message)) {
      fetch("enable",  {
          method: "POST",
          headers: crumb.wrap({}),
        }
      );
      location.reload();
    };
  }
});

Behaviour.specify("#add-button", 'agent-maintenance', 0, function(e) {
    e.onclick = openForm;
});

Behaviour.specify("#edit-button", 'agent-maintenance', 0, function(e) {
    e.onclick = function() {
      location.href='config';
    }
    let table = document.getElementById("maintenance-table");
    let tbody = table.tBodies[0];
    if (tbody.children.length == 0) {
      e.style.display = 'none';
    }
});

Behaviour.specify("#cancel-button", 'agent-maintenance', 0, function(e) {
    e.onclick = closeForm;
});

Behaviour.specify("#delete-selected-button", 'agent-maintenance', 0, function(e) {
    let table = document.getElementById("maintenance-table");
    let tbody = table.tBodies[0];
    if (tbody.children.length == 0) {
      e.style.display = 'none';
    }
});

Behaviour.specify("#select-all", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    selectMaintenanceWindows(true, "");
  }
});


Behaviour.specify("#select-active", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    selectMaintenanceWindows(true, ".active");
  }
});

Behaviour.specify("#select-inactive", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    selectMaintenanceWindows(true, ".inactive");
  }
});

Behaviour.specify("#select-none", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    selectMaintenanceWindows(false, "");
  }
});

Behaviour.specify(".am__checkbox--disabled", 'agent-maintenance', 0, function(e) {
  e.disabled = true;
});


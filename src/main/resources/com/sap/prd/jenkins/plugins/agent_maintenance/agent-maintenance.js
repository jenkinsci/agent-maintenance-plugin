function openForm(formName) {
    document.getElementById(formName).style.display = "flex";
    document.addEventListener("keydown", cancelAdd);
}

function closeForm(formName) {
    document.getElementById(formName).style.display = "none";
    document.removeEventListener("keydown", cancelAdd);
}

function cancelAdd(event) {
    if (event.key === 'Escape') {
        closeForm();
    }
}

function refresh() {
  let table = document.getElementById("maintenance-table");
  let tBody = table.tBodies[0];
  maintenanceJavaScriptBind.getMaintenanceStatus(function(response) {
    let result = response.responseObject();
    for (let rowid = 0; rowid < tBody.rows.length; rowid++) {
      let row = tBody.rows[rowid];
      if (row.id in result) {
        if (result[row.id]) {
          if (row.classList.contains("inactive")) {
            row.classList.remove("inactive");
            row.classList.add("active");
          }
        } else {
          if (row.classList.contains("active")) {
            row.classList.remove("active");
            row.classList.add("inactive");
          }
        }
      } else {
        tBody.removeChild(row);
      }
    }
    if (tBody.children.length == 0) {
      document.getElementById("delete-selected-button-action").style.display = "none";
      document.getElementById("edit-button").style.display = "none";
      document.getElementById("am__div--select").style.display = "none";
    }
  });
}


window.addEventListener("DOMContentLoaded", (event) => {
  window.setInterval(refresh, 20000);
});

var selectMaintenanceWindows = function(toggle, className) {
    let table = document.getElementById("maintenance-table");
    let inputs = table.querySelectorAll('tr' + className + ' input.am__checkbox');
    for (let input of inputs) {
      input.checked = toggle;
    }
};

Behaviour.specify(".am__action-delete", 'agent-maintenance', 0, function(e) {
  e.onclick = function () {
    let row = findAncestor(this, "TR");
    let message = this.getAttribute("data-message");
    let messageSuccess = this.getAttribute("data-message-success");
    let id = row.id;
    dialog.confirm(message).then( () => {
      maintenanceJavaScriptBind.deleteMaintenance(id, function(response) {
        let result = response.responseObject();
        if (result) {
          let tbody = row.parentNode;
          tbody.removeChild(row);
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
          if (tbody.children.length == 0) {
            document.getElementById("edit-button").style.display = "none";
            document.getElementById("delete-selected-button-action").style.display = "none";
            document.getElementById("am__div--select").style.display = "none";
          }
        } else {
          notificationBar.show("Something went wrong. Please check the logs.", notificationBar.ERROR);
        }
      });
    });
  }
});

Behaviour.specify(".am__action-delete-recurring", 'agent-maintenance', 0, function(e) {
  e.onclick = function () {
    let row = findAncestor(this, "TR");
    let message = this.getAttribute("data-message");
    let messageSuccess = this.getAttribute("data-message-success");
    let id = row.id;
    dialog.confirm(message).then( () => {
      maintenanceJavaScriptBind.deleteRecurringMaintenance(id, function(response) {
        let result = response.responseObject();
        if (result) {
          let tbody = row.parentNode;
          tbody.removeChild(row);
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
          if (tbody.children.length == 0) {
            document.getElementById("edit-recurring").style.display = "none";
            document.getElementById("delete-selected-recurring-action").style.display = "none";
            document.getElementById("am__div--select").style.display = "none";
          }
        } else {
          notificationBar.show("Something went wrong. Please check the logs.", notificationBar.ERROR);
        }
      });
    });
  }
});

Behaviour.specify(".am__link-delete", 'agent-maintenance', 0, function(e) {
  e.onclick = function () {
    let message = this.getAttribute("data-message");
    let messageSuccess = this.getAttribute("data-message-success");
    let row = findAncestor(this, "TR");
    let id = row.id;
    let computerName = row.getAttribute("data-computer-name");
    dialog.confirm(message).then( () => {
      maintenanceJavaScriptBind.deleteMaintenance(id, computerName, function(response) {
        let result = response.responseObject();
        if (result) {
          let tbody = row.parentNode;
          tbody.removeChild(row);
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
          if (tbody.children.length == 0) {
            document.getElementById("delete-selected-button-link").style.display = "none";
            document.getElementById("am__div--select").style.display = "none";
          }
        } else {
          notificationBar.show("Something went wrong. Please check the logs.", notificationBar.ERROR);
        }
      });
    });
  }
});

Behaviour.specify(".am__disable", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    let message = this.getAttribute("data-message");
    dialog.confirm(message).then( () => {
      fetch("disable",  {
          method: "POST",
          headers: crumb.wrap({}),
        }
      );
      location.reload();
    });
  }
});

Behaviour.specify(".am__enable", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    let message = this.getAttribute("data-message");
    dialog.confirm(message).then( () => {
      fetch("enable",  {
          method: "POST",
          headers: crumb.wrap({}),
        }
      );
      location.reload();
    });
  }
});

Behaviour.specify("#add-button", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    openForm("maintenance-add-form")
  };
  let table = document.getElementById("maintenance-table");
  let tbody = table.tBodies[0];
  e.style.display = 'inline-flex';
  e.classList.remove("jenkins-hidden");
});

Behaviour.specify("#add-recurring", 'agent-maintenance', 0, function(e) {
    e.onclick = function() {
    openForm("recurring-maintenance-add-form")
    };
});

Behaviour.specify("#edit-button", 'agent-maintenance', 0, function(e) {
    e.onclick = function() {
      location.href='config';
    }
    let table = document.getElementById("maintenance-table");
    let tbody = table.tBodies[0];
    if (tbody.children.length != 0) {
      e.style.display = 'inline-flex';
      e.classList.remove("jenkins-hidden");
    }
});

Behaviour.specify("#edit-recurring", 'agent-maintenance', 0, function(e) {
    e.onclick = function() {
      location.href='config';
    }
    let table = document.getElementById("recurring-maintenance-table");
    let tbody = table.tBodies[0];
    if (tbody.children.length == 0) {
      e.style.display = 'none';
    }
});

Behaviour.specify("#cancel-button", 'agent-maintenance', 0, function(e) {
    e.onclick = function() {
      closeForm("maintenance-add-form");
    }
});

Behaviour.specify("#recurring-cancel-button", 'agent-maintenance', 0, function(e) {
    e.onclick = function() {
      closeForm("recurring-maintenance-add-form");
    }
});

Behaviour.specify("#delete-selected-button-action", 'agent-maintenance', 0, function(e) {
  let table = document.getElementById("maintenance-table");
  let tbody = table.tBodies[0];
  let messageSuccess = e.getAttribute("data-message-success");
  e.onclick = function() {
    let checkedRows = tbody.querySelectorAll("input.am__checkbox:checked");
    let checkedList = [];
    for (let checked of checkedRows) {
      let row = findAncestor(checked, "TR");
      let id = row.id;
      checkedList.push(id);
    }
    if (checkedList.length > 0) {
      maintenanceJavaScriptBind.deleteMultiple(checkedList, function(response) {
        let result = response.responseObject();
        let error = false;
        if (result.length != checkedList.length) {
          error = true;
        }
        for (let id of result) {
          let row = document.getElementById(id);
          tbody.removeChild(row);
        }
        if (error) {
          notificationBar.show("Something went wrong. Please check the logs.", notificationBar.ERROR);
        } else {
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
        }
        if (tbody.children.length == 0) {
          document.getElementById("edit-button").style.display = "none";
          e.style.display = 'none';
        }
      });
    }
  }
  if (tbody.children.length != 0) {
    e.style.display = 'inline-flex';
    e.classList.remove("jenkins-hidden");
    let select = document.getElementById("am__div--select");
    select.classList.remove("jenkins-hidden");
    select.style.display="block";
  }
});

Behaviour.specify("#delete-selected-recurring-action", 'agent-maintenance', 0, function(e) {
  let table = document.getElementById("recurring-maintenance-table");
  let tbody = table.tBodies[0];
  let messageSuccess = e.getAttribute("data-message-success");
  e.onclick = function() {
    let checkedRows = tbody.querySelectorAll("input.am__checkbox:checked");
    let checkedList = [];
    for (let checked of checkedRows) {
      let row = findAncestor(checked, "TR");
      let id = row.id;
      checkedList.push(id);
    }
    if (checkedList.length > 0) {
      maintenanceJavaScriptBind.deleteMultipleRecurring(checkedList, function(response) {
        let result = response.responseObject();
        let error = false;
        if (result.length != checkedList.length) {
          error = true;
        }
        for (let id of result) {
          let row = document.getElementById(id);
          tbody.removeChild(row);
        }
        if (error) {
          notificationBar.show("Something went wrong. Please check the logs.", notificationBar.ERROR);
        } else {
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
        }
        if (tbody.children.length == 0) {
          document.getElementById("edit-recurring").style.display = "none";
          e.style.display = 'none';
        }
      });
    }
  }
  if (tbody.children.length == 0) {
    e.style.display = 'none';
  }
});

Behaviour.specify("#delete-selected-button-link", 'agent-maintenance', 0, function(e) {
  let table = document.getElementById("maintenance-table");
  let tbody = table.tBodies[0];
  let messageSuccess = e.getAttribute("data-message-success");
  e.onclick = function() {
    let checkedRows = tbody.querySelectorAll("input.am__checkbox:checked");
    let checkedList = {};
    let size = 0;
    for (let checked of checkedRows) {
      let row = findAncestor(checked, "TR");
      let id = row.id;
      let computerName = row.getAttribute("data-computer-name");
      checkedList[id] = computerName;
      size++;
    }
    if (size > 0) {
      maintenanceJavaScriptBind.deleteMultiple(checkedList, function(response) {
        let result = response.responseObject();
        let error = false;
        if (result.length != size) {
          error = true;
        }
        for (let id of result) {
          let row = document.getElementById(id);
          tbody.removeChild(row);
        }
        if (error) {
          notificationBar.show("Something went wrong. Please check the logs.", notificationBar.ERROR);
        } else {
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
        }
        if (tbody.children.length == 0) {
          e.style.display = 'none';
        }
      });
    }
  }
  if (tbody.children.length != 0) {
    e.style.display = "inline-flex";
    e.classList.remove("jenkins-hidden")
    let select = document.getElementById("am__div--select");
    select.classList.remove("jenkins-hidden");
    select.style.display="block";
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


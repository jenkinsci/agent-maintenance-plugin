const GENERIC_ERROR = "Something went wrong. Please check the logs.";

function hide(element) {
  if (element != null) {
    element.classList.add("jenkins-hidden");
  }
}

// Hides the select-all container and delete button belonging to the given table.
function hideTableControls(table) {
  if (table == null) {
    return;
  }
  hide(table.querySelector(".jenkins-table__checkbox-container"));
  const form = table.closest("form");
  if (form != null) {
    hide(form.querySelector(".delete-selected-button"));
  }
}

function reloadAfterPost(url) {
  return fetch(url, {
    method: "POST",
    headers: crumb.wrap({}),
  }).then(response => {
    if (response.ok) {
      location.reload();
    } else {
      notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
    }
  }).catch(() => {
    notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
  });
}

function openForm(formName) {
    const formTemplate = document.getElementById(formName);
    const form = formTemplate.firstElementChild.cloneNode(true);
    const title = formTemplate.dataset.title;
    form.classList.remove("no-json");
    dialog.form(form, {
      title: title,
      okText: dialog.translations.add,
      minWidth: "900px",
      submitButton: false,
    }).then(formData => {
      buildFormTree(form);
      // Reloading before the response arrives would abort the request, so wait for it.
      return fetch(form.action, {
        body: new URLSearchParams(new FormData(form)),
        method: "post",
        headers: crumb.wrap({
          "Content-Type": "application/x-www-form-urlencoded",
        }),
      }).then(response => {
        if (response.ok) {
          location.reload();
        } else {
          notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
        }
      }).catch(() => {
        notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
      });
    }, () => {})
}

function refresh() {
  let table = document.getElementById("maintenance-table");
  if (table == null) {
    return;
  }
  let tBody = table.tBodies[0];
  maintenanceJavaScriptBind.getMaintenanceStatus(function(response) {
    let result = response.responseObject();
    for (let rowid = tBody.rows.length - 1; rowid >= 0; rowid--) {
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
      hideTableControls(table);
      hide(document.getElementById("edit-button"));
    }
  });
}

window.addEventListener("DOMContentLoaded", (event) => {
  window.setInterval(refresh, 20000);
});

let selectMaintenanceWindows = function(button, toggle, className) {
  const table = document.getElementById("maintenance-table");
  const inputs = table.querySelectorAll('tr' + className + ' input.am__checkbox');
  for (let input of inputs) {
    input.checked = toggle;
  }
  updateDeleteSelectedButton(table);
  const ev = new CustomEvent("updateIcon", {
    bubbles: true,
  });
  button.dispatchEvent(ev);
};

Behaviour.specify(".am__action-delete", 'agent-maintenance', 0, function(e) {
  e.onclick = function () {
    let row = this.closest("TR");
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
            hide(document.getElementById("edit-button"));
            hideTableControls(document.getElementById("maintenance-table"));
          }
        } else {
          notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
        }
      });
    });
  }
});

Behaviour.specify(".am__action-delete-recurring", 'agent-maintenance', 0, function(e) {
  e.onclick = function () {
    let row = this.closest("TR");
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
            hide(document.getElementById("edit-recurring"));
            hideTableControls(document.getElementById("recurring-maintenance-table"));
          }
        } else {
          notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
        }
      });
    });
  }
});

Behaviour.specify(".am__link-delete", 'agent-maintenance', 0, function(e) {
  e.onclick = function () {
    let message = this.getAttribute("data-message");
    let messageSuccess = this.getAttribute("data-message-success");
    let row = this.closest("TR");
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
            hideTableControls(document.getElementById("maintenance-table"));
          }
        } else {
          notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
        }
      });
    });
  }
});

Behaviour.specify(".am__disable", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    let message = this.getAttribute("data-message");
    dialog.confirm(message).then( () => {
      reloadAfterPost("disable");
    });
  }
});

Behaviour.specify(".am__enable", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    let message = this.getAttribute("data-message");
    dialog.confirm(message).then( () => {
      reloadAfterPost("enable");
    });
  }
});

Behaviour.specify("#add-button", 'agent-maintenance', 0, function(e) {
  e.onclick = function() {
    openForm("maintenance-add-form")
  };
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
});

Behaviour.specify("#edit-recurring", 'agent-maintenance', 0, function(e) {
    e.onclick = function() {
      location.href='config';
    }
    let table = document.getElementById("recurring-maintenance-table");
    let tbody = table.tBodies[0];
    if (tbody.children.length == 0) {
      e.classList.add("jenkins-hidden");
    }
});

const anyCheckboxesSelected = (table) => {
  return (
    table.querySelectorAll("input.am__checkbox:checked:not(:disabled)")
      .length > 0
  );
};

const updateDeleteSelectedButton = (table) => {
  const form = table == null ? null : table.closest("form");
  const deleteSelectedButton = form == null ? null : form.querySelector(".delete-selected-button");
  if (deleteSelectedButton != null) {
    deleteSelectedButton.disabled = !anyCheckboxesSelected(table);
  }
};

Behaviour.specify(".am__table", "agent-maintenance", 0, function(table) {
  const checkboxes = table.querySelectorAll("input.am__checkbox");

  checkboxes.forEach((checkbox) => {
    checkbox.addEventListener("change", () => {
      updateDeleteSelectedButton(table);
    });
  });
});

Behaviour.specify("#delete-selected-button-action", 'agent-maintenance', 0, function(e) {
  let table = document.getElementById("maintenance-table");
  let tbody = table.tBodies[0];
  let messageSuccess = e.getAttribute("data-message-success");
  e.onclick = function() {
    let checkedRows = tbody.querySelectorAll("input.am__checkbox:checked");
    let checkedList = [];
    for (let checked of checkedRows) {
      let row = checked.closest("TR");
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
          notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
        } else {
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
        }
        if (tbody.children.length == 0) {
          hide(document.getElementById("edit-button"));
          hideTableControls(table);
        }
      });
    }
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
      let row = checked.closest("TR");
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
          notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
        } else {
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
        }
        if (tbody.children.length == 0) {
          hide(document.getElementById("edit-recurring"));
          hideTableControls(table);
        }
      });
    }
  }
  if (tbody.children.length == 0) {
    e.classList.add("jenkins-hidden");
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
      let row = checked.closest("TR");
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
          notificationBar.show(GENERIC_ERROR, notificationBar.ERROR);
        } else {
          notificationBar.show(messageSuccess, notificationBar.SUCCESS)
        }
        if (tbody.children.length == 0) {
          hideTableControls(table);
        }
      });
    }
  }
});

Behaviour.specify("[data-select='all'], [data-select='none'], .jenkins-table__checkbox", 'agent-maintenance', 0, function(e) {
  e.addEventListener("click", function() {
    const table = e.closest(".jenkins-table");
    updateDeleteSelectedButton(table);
  });
});

Behaviour.specify("#select-active", 'agent-maintenance', 0, function(e) {
  e.addEventListener("click", function() {
    selectMaintenanceWindows(e, true, ".active");
  });
});

Behaviour.specify("#select-inactive", 'agent-maintenance', 0, function(e) {
  e.addEventListener("click", function() {
    selectMaintenanceWindows(e, true, ".inactive");
  });
});

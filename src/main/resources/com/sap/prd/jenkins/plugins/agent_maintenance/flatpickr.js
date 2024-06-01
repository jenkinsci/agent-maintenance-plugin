 Behaviour.specify(".am__flatpickr", "am-flatpickr", 0, function(fp) {
   flatpickr(fp, {
     allowInput: true,
     enableTime: true,
     wrap: true,
     clickOpens: false,
     dateFormat: "Y-m-d H:i",
     time_24hr: true,
     minDate: fp.dataset.now,
     static: true,
  });
});

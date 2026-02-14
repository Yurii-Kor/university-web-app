(function () {
  document.querySelectorAll('[data-course-edit-cancel]').forEach(btn => {
    btn.addEventListener('click', () => {
      const form = btn.closest('form');
      const details = btn.closest('details');
      if (form) form.reset();
      if (details) details.open = false;
    });
  });
})();

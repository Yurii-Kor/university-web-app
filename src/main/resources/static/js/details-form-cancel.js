(function () {
  document.addEventListener('click', function (event) {
    const btn = event.target.closest('[data-details-form-cancel]');
    if (!btn) return;

    const form = btn.closest('form');
    const details = btn.closest('details');

    if (form) form.reset();
    if (details) details.open = false;
  });
})();
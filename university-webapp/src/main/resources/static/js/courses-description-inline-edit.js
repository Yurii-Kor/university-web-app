(function () {
  function qs(root, sel) { return root.querySelector(sel); }

  document.querySelectorAll('.course-card').forEach(card => {
    const editBtn = qs(card, '[data-desc-edit]');
    const view = qs(card, '[data-desc-view]');
    const form = qs(card, '[data-desc-form]');
    const cancelBtn = qs(card, '[data-desc-cancel]');
    const textarea = form ? qs(form, 'textarea[name="description"]') : null;

    if (!editBtn || !view || !form || !textarea || !cancelBtn) return;

    const originalText = (view.textContent || '').trim() === '—' ? '' : (view.textContent || '');

    editBtn.addEventListener('click', () => {
      const details = card.querySelector('details.course-desc');
      if (details) details.open = true;

      view.style.display = 'none';
      editBtn.style.display = 'none';
      form.style.display = '';

      textarea.value = originalText.trim();
      textarea.focus();
    });

    cancelBtn.addEventListener('click', () => {
      form.style.display = 'none';
      view.style.display = '';
      editBtn.style.display = '';
      textarea.value = originalText.trim();
    });
  });
})();

(function () {
  const btn = document.getElementById('toggleDescriptions');
  if (!btn) return;

  const details = Array.from(document.querySelectorAll('details.course-desc'));
  if (details.length === 0) {
    btn.remove();
    return;
  }

  let opened = false;

  btn.addEventListener('click', function () {
    opened = !opened;

    details.forEach(d => { d.open = opened; });

    btn.textContent = opened ? 'Hide descriptions' : 'Show descriptions';
  });
})();

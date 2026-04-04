(function () {
  // ===== Toggle password =====
  const input = document.getElementById('password');
  const btn = document.getElementById('togglePassword');

  if (input && btn) {
    btn.addEventListener('click', () => {
      const show = input.type === 'password';
      input.type = show ? 'text' : 'password';
      btn.setAttribute('aria-pressed', String(show));
      btn.title = show ? 'Hide password' : 'Show password';
      btn.textContent = show ? '🙈' : '👁';

      input.focus({ preventScroll: true });
      const len = input.value.length;
      try { input.setSelectionRange(len, len); } catch (e) {}
    });
  }

  // ===== Copy helper =====
  async function copyText(text) {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(text);
      return;
    }
    const ta = document.createElement('textarea');
    ta.value = text;
    ta.style.position = 'fixed';
    ta.style.left = '-9999px';
    document.body.appendChild(ta);
    ta.select();
    document.execCommand('copy');
    ta.remove();
  }

  document.addEventListener('click', async (e) => {
    const copyBtn = e.target.closest('.tips-copy');
    if (!copyBtn) return;

    const selector = copyBtn.getAttribute('data-copy');
    const el = selector ? document.querySelector(selector) : null;
    if (!el) return;

    const text = (el.textContent || '').trim();
    if (!text) return;

    try {
      await copyText(text);

      const old = copyBtn.textContent;
      copyBtn.textContent = '✓';
      copyBtn.disabled = true;

      setTimeout(() => {
        copyBtn.textContent = old;
        copyBtn.disabled = false;
      }, 700);
    } catch (err) {
      console.warn('Copy failed', err);
    }
  });
})();

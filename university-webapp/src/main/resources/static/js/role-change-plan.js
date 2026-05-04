document.addEventListener("click", async (event) => {
  const trigger = event.target.closest("[data-role-change-plan-url]");

  if (!trigger) {
    return;
  }

  event.preventDefault();

  const details = trigger.closest("details");
  const planUrl = trigger.dataset.roleChangePlanUrl;
  const restoreFormId = trigger.dataset.roleChangeRestoreFormId;

  try {
    const response = await fetch(planUrl, {
      method: "GET",
      headers: {
        "Accept": "application/json"
      }
    });

    if (!response.ok) {
      details.open = true;
      return;
    }

    const plan = await response.json();

    if (plan.mode === "AUTO_RESTORE_AVAILABLE") {
      const message = plan.message
        ? `${plan.message}\n\nApply role change now?`
        : "Previous profile can be restored. Apply role change now?";

      if (confirm(message)) {
        const restoreForm = document.getElementById(restoreFormId);

        if (restoreForm) {
          restoreForm.submit();
        }
      }

      return;
    }

    if (plan.mode === "BLOCKED") {
      alert(plan.message || "Role change is blocked.");
      return;
    }

    details.open = true;
  } catch (error) {
    details.open = true;
  }
});
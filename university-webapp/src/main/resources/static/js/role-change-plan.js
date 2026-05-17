document.addEventListener("click", async (event) => {
  const trigger = event.target.closest("[data-role-change-assessment-url]");

  if (!trigger) {
    return;
  }

  event.preventDefault();

  const details = trigger.closest("details");
  const assessmentUrl = trigger.dataset.roleChangeAssessmentUrl;
  const restoreFormId = trigger.dataset.roleChangeRestoreFormId;

  try {
    const response = await fetch(assessmentUrl, {
      method: "GET",
      headers: {
        "Accept": "application/json"
      }
    });

    if (!response.ok) {
      details.open = true;
      return;
    }

    const assessment = await response.json();

    if (assessment.mode === "AUTO_RESTORE_AVAILABLE") {
      const message = assessment.message
        ? `${assessment.message}\n\nApply role change now?`
        : "Previous profile can be restored. Apply role change now?";

      if (confirm(message)) {
        const restoreForm = document.getElementById(restoreFormId);

        if (restoreForm) {
          restoreForm.submit();
        }
      }

      return;
    }

    if (assessment.mode === "BLOCKED") {
      alert(assessment.message || "Role change is blocked.");
      return;
    }

    details.open = true;
  } catch (error) {
    details.open = true;
  }
});
export async function saveLatestWorkflow(getCurrent, persist, onSaved) {
  let current = getCurrent();
  while (current) {
    const result = await persist(current);
    onSaved(current, result);
    const latest = getCurrent();
    if (
      !latest ||
      (latest.snapshot === current.snapshot && latest.file === current.file)
    )
      return;
    current = latest;
  }
}

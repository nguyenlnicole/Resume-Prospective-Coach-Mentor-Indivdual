export function visibleReviewFindings(findings, includePassing = false) {
  const items = Array.isArray(findings) ? findings.filter(item => item && typeof item === 'object') : [];
  if (includePassing) return items;
  return [...items.filter(item => item.status !== 'pass'), ...items.filter(item => item.status === 'pass')];
}
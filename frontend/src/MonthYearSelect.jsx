import React from 'react';
import './month-year-select.css';

export default function MonthYearSelect({ id, label, value = '', onChange, required = false }) {
  const [year = '', month = ''] = value.split('-');
  const currentYear = new Date().getFullYear();
  const years = Array.from({ length: currentYear + 11 - 1900 }, (_, index) => currentYear + 10 - index);
  if (year && !years.includes(Number(year))) years.push(Number(year));
  const months = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  const change = (nextYear, nextMonth) => onChange(nextYear || nextMonth ? `${nextYear}-${nextMonth}` : '');
  return <div className="month-year-select" role="group" aria-label={label}>
    <select id={id} aria-label={`${label} month`} required={required} value={month} onChange={event => change(year,event.target.value)}>
      <option value="">Month</option>
      {months.map((name,index) => <option key={name} value={String(index+1).padStart(2,'0')}>{name}</option>)}
    </select>
    <select id={`${id}-year`} aria-label={`${label} year`} required={required} value={year} onChange={event => change(event.target.value,month)}>
      <option value="">Year</option>
      {years.map(item => <option key={item} value={item}>{item}</option>)}
    </select>
  </div>;
}

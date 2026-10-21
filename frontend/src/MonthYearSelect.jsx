import React from 'react';
import './month-year-select.css';

export default function MonthYearSelect({ id, label, value = '', onChange, required = false, allowFuture = false }) {
  const [year = '', month = ''] = value.split('-');
  const today = new Date();
  const currentYear = today.getFullYear();
  const currentMonth = today.getMonth() + 1;
  const lastYear = currentYear + (allowFuture ? 10 : 0);
  const years = Array.from({ length: lastYear + 1 - 1900 }, (_, index) => lastYear - index);
  if (year && Number(year) <= lastYear && !years.includes(Number(year))) years.push(Number(year));
  const months = ['January','February','March','April','May','June','July','August','September','October','November','December'];
  const change = (nextYear, nextMonth) => {
    if (!allowFuture && Number(nextYear) === currentYear && Number(nextMonth) > currentMonth) nextMonth = '';
    onChange(nextYear || nextMonth ? `${nextYear}-${nextMonth}` : '');
  };
  return <div className="month-year-select" role="group" aria-label={label}>
    <select id={id} aria-label={`${label} month`} required={required} value={month} onChange={event => change(year,event.target.value)}>
      <option value="">Month</option>
      {months.map((name,index) => <option key={name} disabled={!allowFuture && Number(year) === currentYear && index + 1 > currentMonth} value={String(index+1).padStart(2,'0')}>{name}</option>)}
    </select>
    <select id={`${id}-year`} aria-label={`${label} year`} required={required} value={year} onChange={event => change(event.target.value,month)}>
      <option value="">Year</option>
      {year && Number(year) > lastYear && <option value={year} disabled>{year} — Choose a past or current date</option>}
      {years.map(item => <option key={item} value={item}>{item}</option>)}
    </select>
  </div>;
}

import { render, screen, within } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';

import { CategoryIconPicker } from '../CategoryIconPicker/CategoryIconPicker.jsx';
import { SurveyPreviewModal } from '../SurveyPreviewModal/SurveyPreviewModal.jsx';
import { SurveyBuilder } from '../SurveyBuilder/SurveyBuilder.jsx';
import { CATEGORY_ICON_OPTIONS, DEFAULT_CATEGORY_ICON } from '../../utils/categoryIconOptions.js';

describe('editores compartidos', () => {
  it('permite escoger un icono y cerrar el menú con Escape', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<CategoryIconPicker label="Icono" value={DEFAULT_CATEGORY_ICON} onChange={onChange} />);
    const trigger = screen.getByRole('button', { name: /^Icono:/ });
    expect(trigger).not.toHaveAttribute('aria-controls');
    await user.click(trigger);
    const menu = screen.getByRole('group', { name: 'Iconos disponibles' });
    expect(document.getElementById(trigger.getAttribute('aria-controls'))).toBe(menu);
    const next = CATEGORY_ICON_OPTIONS.find((option) => option.value !== DEFAULT_CATEGORY_ICON);
    await user.click(within(menu).getByRole('button', { name: next.label }));
    expect(onChange).toHaveBeenCalledWith(next.value);
    await user.click(trigger);
    await user.keyboard('{Escape}');
    expect(screen.queryByRole('group', { name: 'Iconos disponibles' })).not.toBeInTheDocument();
    expect(trigger).not.toHaveAttribute('aria-controls');
  });

  it('ofrece los iconos nuevos al crear una categoría', async () => {
    const user = userEvent.setup();
    const onChange = vi.fn();
    render(<CategoryIconPicker label="Icono" value={DEFAULT_CATEGORY_ICON} onChange={onChange} />);
    await user.click(screen.getByRole('button', { name: /^Icono:/ }));
    const menu = screen.getByRole('group', { name: 'Iconos disponibles' });
    expect(within(menu).getByRole('button', { name: 'Internet' })).toBeVisible();
    await user.click(within(menu).getByRole('button', { name: 'Ideas' }));
    expect(onChange).toHaveBeenCalledWith('BsLightbulb');
  });

  it('previsualiza la encuesta y permite salir', async () => {
    const user = userEvent.setup();
    const onClose = vi.fn();
    render(<SurveyPreviewModal isOpen survey={{ title: 'Encuesta ejemplo' }} structure={{ items: [{ itemType: 'TEXT', textBody: 'Bienvenida' }] }} onClose={onClose} />);
    expect(screen.getByRole('dialog', { name: 'VISTA DEL ENCUESTADO' })).toBeVisible();
    expect(screen.getByRole('heading', { name: 'Encuesta ejemplo' })).toBeVisible();
    expect(screen.getByText('Bienvenida')).toBeVisible();
    await user.click(screen.getAllByRole('button', { name: 'Cerrar previsualización' })[0]);
    expect(onClose).toHaveBeenCalledOnce();
  });

  it('presenta la estructura de encuesta sin información de plantilla', () => {
    render(<SurveyBuilder data={{ items: [] }} onChange={vi.fn()} readOnly />);
    expect(screen.getByRole('heading', { name: 'ESTRUCTURA DE LA ENCUESTA' })).toBeVisible();
    expect(screen.queryByRole('heading', { name: 'INFORMACIÓN GENERAL' })).not.toBeInTheDocument();
  });
});

import { readFileSync } from 'node:fs';
import { resolve } from 'node:path';
import process from 'node:process';
import { describe, expect, it } from 'vitest';

const css = readFileSync(resolve(process.cwd(), 'src/assets/styles/global.css'), 'utf8');

function color(name) {
  const value = css.match(new RegExp(`--${name}:\\s*(#[0-9a-fA-F]{6})`))?.[1];
  if (!value) throw new Error(`Falta el color --${name}`);
  return value;
}

function luminance(hex) {
  const channels = hex.slice(1).match(/.{2}/g).map((part) => {
    const value = parseInt(part, 16) / 255;
    return value <= 0.04045 ? value / 12.92 : ((value + 0.055) / 1.055) ** 2.4;
  });

  return channels[0] * 0.2126 + channels[1] * 0.7152 + channels[2] * 0.0722;
}

function contrast(foreground, background) {
  const values = [luminance(foreground), luminance(background)].sort((a, b) => b - a);
  return (values[0] + 0.05) / (values[1] + 0.05);
}

describe('contraste de la paleta compartida', () => {
  it.each([
    ['texto sobre la página', 'color-text', 'color-background'],
    ['texto secundario sobre la página', 'color-text-muted', 'color-background'],
    ['texto secundario sobre tarjetas', 'color-text-muted', 'color-surface'],
    ['enlaces y botones claros', 'color-primary', 'color-surface'],
    ['enlaces sobre la página', 'color-primary', 'color-background'],
    ['errores sobre su aviso', 'color-error', 'color-error-surface'],
    ['advertencias sobre su aviso', 'color-warning', 'color-warning-surface'],
    ['acentos administrativos sobre su tarjeta', 'color-super', 'color-super-surface'],
  ])('%s tiene contraste mejorado para texto normal', (_label, foreground, background) => {
    expect(contrast(color(foreground), color(background))).toBeGreaterThanOrEqual(7);
  });

  it.each([
    ['botones con el color oficial', 'color-on-action', 'color-action'],
    ['hover de los botones', 'color-on-action', 'color-action-hover'],
    ['letras blancas del header azul', 'color-text-on-dark', 'color-header-background'],
    ['botones blancos del header', 'color-primary', 'color-surface'],
  ])('el texto de %s supera 4.5:1', (_label, foreground, background) => {
    expect(contrast(color(foreground), color(background))).toBeGreaterThanOrEqual(4.5);
  });

  it('el texto grande y en negrita del banner supera 3:1', () => {
    expect(contrast(color('color-on-hero'), color('color-hero'))).toBeGreaterThanOrEqual(3);
  });

  it('el borde oscuro de los campos supera 3:1 sobre el fondo', () => {
    expect(contrast(color('color-primary'), color('color-surface'))).toBeGreaterThanOrEqual(3);
  });

  it('el banner se distingue del fondo gris', () => {
    expect(contrast(color('color-hero'), color('color-background'))).toBeGreaterThanOrEqual(3);
  });

  it('el icono claro del header se distingue del azul', () => {
    expect(contrast(color('color-brand-cream'), color('color-header-background'))).toBeGreaterThanOrEqual(3);
  });

  it('los botones usan la variante oscura y texto blanco', () => {
    expect(color('color-brand')).toBe('#00988d');
    expect(color('color-action')).toBe('#008278');
    expect(color('color-on-action')).toBe('#ffffff');
  });

  it.each(['color-surface', 'color-background', 'color-accent-surface'])(
    'los iconos de marca superan 3:1 sobre %s', (background) => {
      expect(contrast(color('color-brand'), color(background))).toBeGreaterThanOrEqual(3);
    },
  );
});

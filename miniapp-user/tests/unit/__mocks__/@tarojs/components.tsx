import { createElement, forwardRef } from 'react';

export const View = forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  (props, ref) => createElement('div', { ref, ...props }),
);
View.displayName = 'View';

export const Text = forwardRef<HTMLSpanElement, React.HTMLAttributes<HTMLSpanElement>>(
  (props, ref) => createElement('span', { ref, ...props }),
);
Text.displayName = 'Text';

export const Image = forwardRef<HTMLImageElement, React.ImgHTMLAttributes<HTMLImageElement>>(
  (props, ref) => createElement('img', { ref, ...props }),
);
Image.displayName = 'Image';

export const Button = forwardRef<HTMLButtonElement, React.ButtonHTMLAttributes<HTMLButtonElement>>(
  (props, ref) => createElement('button', { ref, ...props }),
);
Button.displayName = 'Button';

export const Input = forwardRef<HTMLInputElement, React.InputHTMLAttributes<HTMLInputElement>>(
  (props, ref) => createElement('input', { ref, ...props }),
);
Input.displayName = 'Input';

export const Textarea = forwardRef<HTMLTextAreaElement, React.TextareaHTMLAttributes<HTMLTextAreaElement>>(
  (props, ref) => createElement('textarea', { ref, ...props }),
);
Textarea.displayName = 'Textarea';

export const ScrollView = forwardRef<HTMLDivElement, React.HTMLAttributes<HTMLDivElement>>(
  (props, ref) => createElement('div', { ref, ...props }),
);
ScrollView.displayName = 'ScrollView';

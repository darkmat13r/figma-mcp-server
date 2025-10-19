import { useState, useEffect } from 'react';
import { SelectionInfo } from '../types';

export function useSelection() {
  const [selection, setSelection] = useState<SelectionInfo[]>([]);

  useEffect(() => {
    const handleMessage = (event: MessageEvent) => {
      const message = event.data.pluginMessage;
      if (message?.type === 'selection-changed') {
        setSelection(message.selection);
      }
    };

    window.addEventListener('message', handleMessage);

    // Request initial selection
    parent.postMessage(
      {
        pluginMessage: {
          type: 'get-selection',
        },
      },
      '*'
    );

    return () => window.removeEventListener('message', handleMessage);
  }, []);

  return selection;
}

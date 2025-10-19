import React, { useEffect, useRef } from 'react';
import { Section } from './composed/Section';
import { ScrollArea } from './ui/scroll-area';
import { Badge } from './ui/badge';
import { Button } from './ui/button';

interface ConsoleLog {
  id: number;
  timestamp: Date;
  type: 'sent' | 'received' | 'error' | 'info';
  data: any;
}

interface ConsolePanelProps {
  logs: ConsoleLog[];
  onClear: () => void;
}

export const ConsolePanel: React.FC<ConsolePanelProps> = ({ logs, onClear }) => {
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto-scroll to bottom when new logs arrive
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [logs]);

  const getTypeColor = (type: ConsoleLog['type']) => {
    switch (type) {
      case 'sent':
        return 'bg-blue-500/10 text-blue-400 border-blue-500/20';
      case 'received':
        return 'bg-green-500/10 text-green-400 border-green-500/20';
      case 'error':
        return 'bg-red-500/10 text-red-400 border-red-500/20';
      case 'info':
        return 'bg-yellow-500/10 text-yellow-400 border-yellow-500/20';
      default:
        return 'bg-muted text-muted-foreground';
    }
  };

  const getTypeLabel = (type: ConsoleLog['type']) => {
    switch (type) {
      case 'sent':
        return '→ SENT';
      case 'received':
        return '← RECV';
      case 'error':
        return '✕ ERROR';
      case 'info':
        return 'ℹ INFO';
    }
  };

  const formatData = (data: any): string => {
    if (typeof data === 'string') {
      return data;
    }
    return JSON.stringify(data, null, 2);
  };

  const formatTimestamp = (date: Date): string => {
    const hours = String(date.getHours()).padStart(2, '0');
    const minutes = String(date.getMinutes()).padStart(2, '0');
    const seconds = String(date.getSeconds()).padStart(2, '0');
    const ms = String(date.getMilliseconds()).padStart(3, '0');
    return `${hours}:${minutes}:${seconds}.${ms}`;
  };

  return (
    <Section
      title="Console"
      description="WebSocket message logs"
      action={
        <Button
          variant="outline"
          size="sm"
          onClick={onClear}
          disabled={logs.length === 0}
        >
          Clear
        </Button>
      }
    >
      <ScrollArea className="h-[300px] w-full rounded-md border border-border bg-muted/20 p-2">
        <div ref={scrollRef} className="space-y-2">
          {logs.length === 0 ? (
            <div className="flex h-[280px] items-center justify-center text-center text-sm text-muted-foreground">
              No messages yet. Connect to the WebSocket server to see logs.
            </div>
          ) : (
            logs.map((log) => (
              <div
                key={log.id}
                className="rounded-md border border-border bg-background p-2 font-mono text-xs"
              >
                <div className="mb-1 flex items-center justify-between gap-2">
                  <Badge
                    variant="outline"
                    className={`${getTypeColor(log.type)} font-semibold`}
                  >
                    {getTypeLabel(log.type)}
                  </Badge>
                  <span className="text-muted-foreground">
                    {formatTimestamp(log.timestamp)}
                  </span>
                </div>
                <pre className="whitespace-pre-wrap break-all text-foreground">
                  {formatData(log.data)}
                </pre>
              </div>
            ))
          )}
        </div>
      </ScrollArea>
    </Section>
  );
};
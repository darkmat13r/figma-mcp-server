import React, { useEffect, useRef, useState } from 'react';
import { ScrollArea } from './ui/scroll-area';
import { Badge } from './ui/badge';
import { Button } from './ui/button';
import { Collapsible, CollapsibleContent, CollapsibleTrigger } from './ui/collapsible';
import { ChevronDown, ChevronUp, Terminal } from 'lucide-react';

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
  const [isOpen, setIsOpen] = useState(false);

  // Auto-scroll to bottom when new logs arrive
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [logs]);

  const getTypeColor = (type: ConsoleLog['type']) => {
    switch (type) {
      case 'sent':
        return 'bg-primary/10 text-primary border-primary/20';
      case 'received':
        return 'bg-accent/20 text-accent-foreground border-accent/30';
      case 'error':
        return 'bg-destructive/10 text-destructive border-destructive/20';
      case 'info':
        return 'bg-muted text-muted-foreground border-muted';
      default:
        return 'bg-muted text-muted-foreground border-border';
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
    <Collapsible open={isOpen} onOpenChange={setIsOpen} className="border-b border-border bg-card">
      <div className="p-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Terminal className="h-4 w-4 text-muted-foreground" />
            <h3 className="text-sm font-medium">Console</h3>
            {logs.length > 0 && (
              <Badge variant="secondary" className="h-5 px-2 text-xs">
                {logs.length}
              </Badge>
            )}
          </div>
          <div className="flex items-center gap-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={onClear}
              disabled={logs.length === 0}
              className="h-7 text-xs"
            >
              Clear
            </Button>
            <CollapsibleTrigger asChild>
              <Button variant="ghost" size="sm" className="h-8 w-8 p-0">
                {isOpen ? <ChevronUp className="h-4 w-4" /> : <ChevronDown className="h-4 w-4" />}
              </Button>
            </CollapsibleTrigger>
          </div>
        </div>
        <p className="mt-1 text-xs text-muted-foreground">
          WebSocket message logs
        </p>
      </div>

      <CollapsibleContent>
        <div className="px-4 pb-4">
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
                    className="rounded-md border border-border bg-card p-2 font-mono text-xs"
                  >
                    <div className="mb-1 flex items-center justify-between gap-2">
                      <Badge
                        variant="outline"
                        className={`${getTypeColor(log.type)} font-semibold text-[10px] px-2 py-0.5`}
                      >
                        {getTypeLabel(log.type)}
                      </Badge>
                      <span className="text-[10px] text-muted-foreground">
                        {formatTimestamp(log.timestamp)}
                      </span>
                    </div>
                    <pre className="mt-1 whitespace-pre-wrap break-all text-foreground text-[11px] leading-relaxed">
                      {formatData(log.data)}
                    </pre>
                  </div>
                ))
              )}
            </div>
          </ScrollArea>
        </div>
      </CollapsibleContent>
    </Collapsible>
  );
};
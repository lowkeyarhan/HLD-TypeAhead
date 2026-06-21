import { errorColor, successColor } from "@/components/dashboard/utils";

type StatusPillProps = {
  online: boolean;
};

export function StatusPill({ online }: StatusPillProps) {
  return (
    <div className="inline-flex items-center gap-2 rounded-full bg-[hsl(var(--bg-elevated))] px-4 py-2 text-[0.88rem] text-[hsl(var(--text-secondary))]">
      <span
        className="inline-block h-2.5 w-2.5 rounded-full"
        style={{ backgroundColor: online ? successColor : errorColor }}
      />
      {online ? "Backend connected" : "Backend unavailable"}
    </div>
  );
}
